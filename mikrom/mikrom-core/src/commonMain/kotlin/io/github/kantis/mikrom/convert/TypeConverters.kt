package io.github.kantis.mikrom.convert

import kotlin.reflect.KClass

public class TypeConverters(
   private val converters: Map<ConverterKey, (Any) -> Any>,
) {
   public data class ConverterKey(val source: KClass<*>, val target: KClass<*>)

   public fun convert(
      value: Any,
      target: KClass<*>,
   ): Any? = converters[ConverterKey(value::class, target)]?.invoke(value)

   public operator fun plus(other: TypeConverters): TypeConverters = TypeConverters(converters + other.converters)

   public companion object {
      public val EMPTY: TypeConverters = TypeConverters(emptyMap())
   }

   public class Builder {
      @PublishedApi
      internal val converters: MutableMap<ConverterKey, (Any) -> Any> = mutableMapOf()

      @Suppress("UNCHECKED_CAST")
      public inline fun <reified S : Any, reified T : Any> register(noinline converter: (S) -> T) {
         converters[ConverterKey(S::class, T::class)] = converter as (Any) -> Any
      }

      public fun build(): TypeConverters = TypeConverters(converters.toMap())
   }
}
