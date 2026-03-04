package io.github.kantis.mikrom

import io.github.kantis.mikrom.convert.TypeConversions
import io.github.kantis.mikrom.convert.defaultConversions
import io.github.kantis.mikrom.generator.NamingStrategy
import io.github.kantis.mikrom.internal.compiledParameterMapper
import io.github.kantis.mikrom.internal.compiledRowMapper
import kotlin.reflect.KClass

public class Mikrom(
   public val rowMappers: MutableMap<KClass<*>, RowMapper<*>>,
   public val parameterMappers: MutableMap<KClass<*>, ParameterMapper<*>> = mutableMapOf(),
   public val conversions: TypeConversions = defaultConversions(),
   public val namingStrategy: NamingStrategy = NamingStrategy.SNAKE_CASE,
) {
   @Suppress("UNCHECKED_CAST")
   public inline fun <reified T : Any> resolveRowMapper(): RowMapper<T> =
      rowMappers[T::class] as? RowMapper<T>
         ?: T::class.compiledRowMapper()
         ?: error("No RowMapper found for type ${T::class}. Please register a RowMapper or ensure it is compiled with the Mikrom plugin.")

   @Suppress("UNCHECKED_CAST")
   public inline fun <reified T : Any> resolveParameterMapper(): ParameterMapper<T> =
      parameterMappers[T::class] as? ParameterMapper<T>
         ?: T::class.compiledParameterMapper()
         ?: error(
            "No ParameterMapper found for type ${T::class}. Please register a ParameterMapper or ensure it is compiled with the Mikrom plugin.",
         )

   public companion object {
      public operator fun invoke(builder: MikromBuilder.() -> Unit): Mikrom = MikromBuilder().apply(builder).build()
   }
}
