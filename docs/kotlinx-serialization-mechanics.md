# KotlinX Serialization: IR Generation & Runtime Discovery Mechanics

This document explains how the KotlinX Serialization compiler plugin generates code and how
serializers are discovered at runtime. The goal is to understand the pattern well enough to
replicate it for generated row mappers in LedgerBoss.

## Overview

The serialization plugin operates at two compiler levels:

1. **FIR (Frontend IR)** - Declares synthetic members (companion objects, nested classes, methods)
   so they're visible during type-checking and resolution.
2. **IR (Backend)** - Fills in the bodies of those declarations with actual serialization logic.

At runtime, serializers are found through a well-defined discovery chain that starts with
companion object methods and falls back to reflection.

---

## Part 1: What Gets Generated

For a class like:

```kotlin
@Serializable
data class User(val name: String, val age: Int)
```

The plugin generates:

### 1.1 Companion Object with `serializer()` Method

If the class has no companion, one is created. A `serializer()` method is added that returns
`KSerializer<User>`:

```kotlin
companion object {
   fun serializer(): KSerializer<User> = $serializer()
   // For generic classes: fun serializer(vararg typeSerializers: KSerializer<*>): KSerializer<User<*>>
}
```

For classes with `@KeepGeneratedSerializer` (using a custom serializer), an additional
`generatedSerializer()` method is generated.

### 1.2 `$serializer` Nested Class

A nested object named `$serializer` that implements `GeneratedSerializer<User>`:

```kotlin
internal object `$serializer` : GeneratedSerializer<User> {
   override val descriptor: SerialDescriptor = ...
   override fun serialize(encoder: Encoder, value: User) { ... }
   override fun deserialize(decoder: Decoder): User { ... }
   override fun childSerializers(): Array<KSerializer<*>> = ...
   // For generic classes: override fun typeParametersSerializers(): Array<KSerializer<*>> = ...
}
```

### 1.3 Synthetic Deserialization Constructor

A special constructor used by the deserializer, with a `SerializationConstructorMarker` parameter
to distinguish it from user-defined constructors:

```kotlin
// Synthetic - not visible in source
constructor(seen0: Int, name: String?, age: Int, marker: SerializationConstructorMarker?)
```

The `seen0` parameter is a bitmask tracking which fields were present in the serialized data.
The constructor validates that all required fields are present using a "golden mask" check.

### 1.4 `write$Self` Static Method (JVM only)

A `@JvmStatic` method on the class that serializes all properties:

```kotlin
@JvmStatic
internal fun `write$Self`(self: User, encoder: CompositeEncoder, descriptor: SerialDescriptor)
```

### 1.5 Cached Properties

- `$cachedSerializer` - lazily initialized serializer instance (non-generic classes)
- `$cachedDescriptor` - cached `SerialDescriptor`
- `$childSerializers` - cached array of child serializers

---

## Part 2: Plugin Architecture

### 2.1 Registration

**Entry point:**
`SerializationComponentRegistrar` (implements `CompilerPluginRegistrar`)

```
plugins/kotlinx-serialization/kotlinx-serialization.cli/src/.../extensions/SerializationComponentRegistrar.kt
```

Registers:
- `FirSerializationExtensionRegistrar` - FIR-level declaration generation
- `SerializationLoweringExtension` - IR-level code generation (implements `IrGenerationExtension`)
- `SerializationResolveExtension` - K1 resolve extension (legacy)

### 2.2 FIR Phase: Declaration Generation

**File:** `kotlinx-serialization.k2/src/.../fir/SerializationFirResolveExtension.kt`
**Class:** `SerializationFirResolveExtension extends FirDeclarationGenerationExtension`

This phase creates the *shape* of generated declarations (without bodies):

```kotlin
override fun getNestedClassifiersNames(classSymbol, context): Set<Name> {
   // Returns names of classes to generate:
   // - DEFAULT_NAME_FOR_COMPANION_OBJECT (if class needs companion)
   // - SERIALIZER_CLASS_NAME ("$serializer")
}

override fun generateNestedClassLikeDeclaration(owner, name, context) {
   // Creates either the companion object or $serializer class
}

override fun getCallableNamesForClass(classSymbol, context): Set<Name> {
   // Returns names of methods/properties to generate:
   // For companion: "serializer", optionally "generatedSerializer"
   // For $serializer: "serialize", "deserialize", "descriptor", etc.
}
```

**Predicate matching** is used to identify `@Serializable` classes:

```kotlin
// Defined in FirSerializationPredicates.kt
annotatedWithSerializableOrMeta = annotated(@Serializable) OR metaAnnotated(@MetaSerializable)
```

**Supertype injection** (`SerializationFirSupertypesExtension`):
- `$serializer` classes get `GeneratedSerializer<T>` or `KSerializer<T>` as supertype
- Companion objects / serializable objects get `SerializerFactory` interface (non-JVM)

### 2.3 IR Phase: Code Generation

**File:** `kotlinx-serialization.backend/src/.../extensions/SerializationLoweringExtension.kt`
**Class:** `SerializationLoweringExtension extends IrGenerationExtension`

Two-pass approach:

```kotlin
override fun generate(moduleFragment, pluginContext) {
   val pass1 = SerializerClassPreLowering(pluginContext)   // declaration stubs
   val pass2 = SerializerClassLowering(pluginContext, ...)  // function bodies
   moduleFragment.files.forEach(pass1::runOnFileInOrder)
   moduleFragment.files.forEach(pass2::runOnFileInOrder)
}
```

**Pass 1** (`IrPreGenerator`): Creates method/constructor signatures without bodies:
- `write$Self` method signature
- Synthetic deserialization constructor signature

**Pass 2** (`SerializerClassLowering`): Fills in all bodies:

```kotlin
override fun lower(irClass: IrClass) {
   SerializableIrGenerator.generate(irClass, context)          // write$Self body, constructor body
   SerializerIrGenerator.generate(irClass, context, ...)       // $serializer methods
   SerializableCompanionIrGenerator.generate(irClass, context) // companion serializer() body
}
```

Key generators:
- `SerializableIrGenerator` - generates the synthetic constructor body and `write$Self` body
- `SerializerIrGenerator` - generates `$serializer.serialize()`, `deserialize()`, descriptor init
- `SerializableCompanionIrGenerator` - generates `Companion.serializer()` body

### 2.4 Intrinsic Optimization (JVM)

The plugin replaces `serializer<T>()` calls at the bytecode level with direct serializer
construction, bypassing the runtime discovery chain entirely.

**File:** `kotlinx-serialization.backend/src/.../backend/ir/SerializationJvmIrIntrinsicSupport.kt`

When the compiler sees `serializer<User>()`, instead of emitting a call to the runtime function,
it emits bytecode that directly calls `User.Companion.serializer()`. This is done through the
`IrIntrinsicExtension` / `JvmIrIntrinsicExtension` mechanism.

This is controlled by the `disableIntrinsic` CLI flag and requires the runtime to have the
`noCompiledSerializer` function available.

---

## Part 3: Runtime Serializer Discovery

When the intrinsic optimization is not available (e.g., reflection-based lookup), the runtime
uses a multi-step discovery chain.

### 3.1 Entry Points

**File:** `kotlinx.serialization/core/commonMain/src/kotlinx/serialization/Serializers.kt`

```kotlin
// Reified version - most common entry point
inline fun <reified T> serializer(): KSerializer<T> =
   serializer(typeOf<T>()).cast()

// KType version
fun serializer(type: KType): KSerializer<Any?> =
   EmptySerializersModule().serializer(type)

// SerializersModule-aware version
fun SerializersModule.serializer(type: KType): KSerializer<Any?>
```

### 3.2 Discovery Chain

The full resolution order in `SerializersModule.serializerByKTypeImpl()`:

```
1. Cache check (SERIALIZERS_CACHE / PARAMETRIZED_SERIALIZERS_CACHE)
   |
2. Built-in serializer? (BUILTIN_SERIALIZERS map - primitives, collections, etc.)
   |
3. Compiled serializer via KClass.compiledSerializerImpl()
   -> delegates to KClass.constructSerializerForGivenTypeArgs()
   |
4. Contextual serializer? (SerializersModule.getContextual())
   |
5. Polymorphic fallback (for interfaces)
   |
6. SerializationException thrown
```

### 3.3 JVM Compiled Serializer Discovery

**File:** `kotlinx.serialization/core/jvmMain/src/kotlinx/serialization/internal/Platform.kt`

The `constructSerializerForGivenTypeArgs()` function on JVM uses Java reflection:

```kotlin
fun <T: Any> Class<T>.constructSerializerForGivenTypeArgs(
   vararg args: KSerializer<Any?>
): KSerializer<T>? {
   // 1. Unannotated enum -> EnumSerializer
   if (isEnum && isNotAnnotated()) return createEnumSerializer()

   // 2. Default companion: get "Companion" field, call serializer() via reflection
   val serializer = invokeSerializerOnDefaultCompanion(this, *args)
   if (serializer != null) return serializer

   // 3. Serializable object: find INSTANCE field, call serializer()
   findObjectSerializer()?.let { return it }

   // 4. Named companion: find field annotated with @NamedCompanion
   val fromNamedCompanion = findInNamedCompanion(*args)
   if (fromNamedCompanion != null) return fromNamedCompanion

   // 5. Polymorphic fallback
   return if (isPolymorphicSerializer()) PolymorphicSerializer(this.kotlin) else null
}
```

The companion invocation (step 2) works like this:

```kotlin
private fun invokeSerializerOnDefaultCompanion(jClass, vararg args): KSerializer<T>? {
   val companion = jClass.getDeclaredField("Companion").get(null) ?: return null
   val types = Array(args.size) { KSerializer::class.java }
   return companion.javaClass.getDeclaredMethod("serializer", *types).invoke(companion, *args)
}
```

For named companions (step 4), a fallback also tries to find the `$serializer` singleton directly:

```kotlin
declaredClasses.singleOrNull { it.simpleName == "$serializer" }
   ?.getField("INSTANCE")?.get(null) as? KSerializer<T>
```

### 3.4 Caching

**File:** `kotlinx.serialization/core/commonMain/src/kotlinx/serialization/SerializersCache.kt`

Four caches by type:
- `SERIALIZERS_CACHE` - non-null, non-parametrized
- `SERIALIZERS_CACHE_NULLABLE` - nullable versions
- `PARAMETRIZED_SERIALIZERS_CACHE` - parametrized types
- `PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE` - nullable parametrized

### 3.5 SerializersModule

**File:** `kotlinx.serialization/core/commonMain/src/kotlinx/serialization/modules/SerializersModule.kt`

Provides contextual and polymorphic serializer registration:

```kotlin
sealed class SerializersModule {
   abstract fun getContextual(kClass: KClass<*>, typeArgumentsSerializers: List<KSerializer<*>>): KSerializer<*>?
   abstract fun getPolymorphic(baseClass: KClass<*>, value: Any): SerializationStrategy<*>?
   abstract fun getPolymorphic(baseClass: KClass<*>, serializedClassName: String?): DeserializationStrategy<*>?
}
```

The implementation (`SerialModuleImpl`) uses hash maps:
- `class2ContextualFactory: Map<KClass<*>, ContextualProvider>` for contextual lookup
- `polyBase2Serializers` for polymorphic serialization
- `polyBase2NamedSerializers` for polymorphic deserialization

---

## Part 4: Key Takeaways for Row Mapper Generation

### The Pattern

1. **Annotation triggers generation**: `@Serializable` -> `@RowMappable` (or similar)
2. **Companion object as discovery anchor**: The compiler plugin adds a `serializer()` method
   to the companion object. This is the primary lookup mechanism. Equivalent: `rowMapper()`.
3. **Nested `$serializer` class**: Contains the actual implementation. Equivalent: `$rowMapper`.
4. **Synthetic constructor**: For deserialization. Equivalent for row mapping: constructor that
   takes a `ResultSet` or similar.
5. **Runtime discovery via reflection**: Falls back to finding the companion method via
   `getDeclaredMethod`. Same pattern works for row mappers.
6. **Intrinsic optimization**: At compile time, replace `rowMapper<T>()` calls with direct
   companion calls, avoiding reflection entirely.
7. **Caching**: Wrap discovery in a cache keyed by `KClass` or `KType`.

### Minimal Viable Approach

The simplest version of this for row mappers:

1. **FIR extension** (`FirDeclarationGenerationExtension`):
   - On classes annotated with your annotation, declare a companion object (if missing)
     and a `rowMapper()` method on it
   - Optionally declare a `$rowMapper` nested class

2. **IR extension** (`IrGenerationExtension`):
   - Generate the body of `rowMapper()` that constructs a `RowMapper<T>` implementation
   - The `RowMapper` reads columns from a `ResultSet`/`Row` and calls the class constructor

3. **Runtime discovery**:
   - `inline fun <reified T> rowMapper(): RowMapper<T>` that calls
     `T::class.companionObject.rowMapper()` via reflection
   - Cache results in a `ConcurrentHashMap<KClass<*>, RowMapper<*>>`
   - Optionally add intrinsic support to replace the call at compile time

### Key Source Files Reference

| Component | Path |
|-----------|------|
| Plugin registration | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.cli/src/.../SerializationComponentRegistrar.kt` |
| FIR declaration gen | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/.../fir/SerializationFirResolveExtension.kt` |
| FIR predicates | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/.../fir/FirSerializationPredicates.kt` |
| FIR supertype injection | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/.../fir/SerializationFirSupertypesExtension.kt` |
| IR lowering entry | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/.../extensions/SerializationLoweringExtension.kt` |
| IR pre-generation | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/.../backend/ir/IrPreGenerator.kt` |
| Serializable class gen | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/.../backend/ir/SerializableIrGenerator.kt` |
| Serializer class gen | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/.../backend/ir/SerializerIrGenerator.kt` |
| Companion gen | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/.../backend/ir/SerializableCompanionIrGenerator.kt` |
| Intrinsic support | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/.../backend/ir/SerializationJvmIrIntrinsicSupport.kt` |
| Naming conventions | `kotlin/plugins/kotlinx-serialization/kotlinx-serialization.common/src/.../resolve/NamingConventions.kt` |
| Runtime serializers | `kotlinx.serialization/core/commonMain/src/kotlinx/serialization/Serializers.kt` |
| JVM platform discovery | `kotlinx.serialization/core/jvmMain/src/kotlinx/serialization/internal/Platform.kt` |
| Serializers cache | `kotlinx.serialization/core/commonMain/src/kotlinx/serialization/SerializersCache.kt` |
| SerializersModule | `kotlinx.serialization/core/commonMain/src/kotlinx/serialization/modules/SerializersModule.kt` |
