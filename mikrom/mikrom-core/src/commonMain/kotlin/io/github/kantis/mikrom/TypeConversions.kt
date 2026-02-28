package io.github.kantis.mikrom

import kotlin.reflect.KClass

public class TypeConversions(
   private val conversions: Map<ConversionKey, (Any) -> Any>,
) {
   public data class ConversionKey(val source: KClass<*>, val target: KClass<*>)

   public fun convert(
      value: Any,
      target: KClass<*>,
   ): Any? = conversions[ConversionKey(value::class, target)]?.invoke(value)

   public operator fun plus(other: TypeConversions): TypeConversions = TypeConversions(conversions + other.conversions)

   public companion object {
      public val EMPTY: TypeConversions = TypeConversions(emptyMap())
   }

   public class Builder {
      @PublishedApi
      internal val conversions: MutableMap<ConversionKey, (Any) -> Any> = mutableMapOf()

      @Suppress("UNCHECKED_CAST")
      public inline fun <reified S : Any, reified T : Any> register(noinline conversion: (S) -> T) {
         conversions[ConversionKey(S::class, T::class)] = conversion as (Any) -> Any
      }

      public fun build(): TypeConversions = TypeConversions(conversions.toMap())
   }
}
