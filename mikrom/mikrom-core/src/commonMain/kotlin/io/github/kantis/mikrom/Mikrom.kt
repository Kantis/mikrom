package io.github.kantis.mikrom

import io.github.kantis.mikrom.internal.compiledRowMapper
import kotlin.reflect.KClass

public class Mikrom(
   public val rowMappers: MutableMap<KClass<*>, RowMapper<*>>,
   public val conversions: TypeConversions = defaultConversions(),
) {
   @Suppress("UNCHECKED_CAST")
   public inline fun <reified T : Any> resolveRowMapper(): RowMapper<T> =
      rowMappers[T::class] as? RowMapper<T>
         ?: T::class.compiledRowMapper()
         ?: error("No RowMapper found for type ${T::class}. Please register a RowMapper or ensure it is compiled with the Mikrom plugin.")

   public companion object {
      public operator fun invoke(builder: MikromBuilder.() -> Unit): Mikrom = MikromBuilder().apply(builder).build()
   }
}
