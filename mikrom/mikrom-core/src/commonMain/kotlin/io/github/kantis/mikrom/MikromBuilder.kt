package io.github.kantis.mikrom

import kotlin.reflect.KClass

public class MikromBuilder {
   public val rowMappers: MutableMap<KClass<*>, RowMapper<*>> = mutableMapOf()

   @PublishedApi
   internal val conversionsBuilder: TypeConversions.Builder = TypeConversions.Builder()

   public inline fun <reified T> registerRowMapper(mapper: RowMapper<T>) {
      rowMappers[T::class] = mapper
   }

   public inline fun <reified S : Any, reified T : Any> registerConversion(noinline conversion: (S) -> T) {
      conversionsBuilder.register(conversion)
   }

   public fun build(): Mikrom = Mikrom(rowMappers, defaultConversions() + conversionsBuilder.build())
}
