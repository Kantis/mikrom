package io.github.kantis.mikrom

import io.github.kantis.mikrom.convert.TypeConverters
import io.github.kantis.mikrom.convert.defaultConverters
import io.github.kantis.mikrom.generator.NamingStrategy
import kotlin.reflect.KClass

public class MikromBuilder {
   public val rowMappers: MutableMap<KClass<*>, RowMapper<*>> = mutableMapOf()
   public val parameterMappers: MutableMap<KClass<*>, ParameterMapper<*>> = mutableMapOf()
   public var namingStrategy: NamingStrategy = NamingStrategy.SNAKE_CASE

   @PublishedApi
   internal val convertersBuilder: TypeConverters.Builder = TypeConverters.Builder()

   public inline fun <reified T> registerRowMapper(mapper: RowMapper<T>) {
      rowMappers[T::class] = mapper
   }

   public inline fun <reified T> registerRowMapper(noinline mapper: context(Mikrom) (Row) -> T) {
      rowMappers[T::class] = RowMapper { row, mikrom -> mapper(mikrom, row) }
   }

   public inline fun <reified T> registerParameterMapper(mapper: ParameterMapper<T>) {
      parameterMappers[T::class] = mapper
   }

   public inline fun <reified T> registerParameterMapper(noinline mapper: (T) -> Map<String, Any?>) {
      parameterMappers[T::class] = ParameterMapper(mapper)
   }

   public inline fun <reified S : Any, reified T : Any> registerConverter(noinline conversion: (S) -> T) {
      convertersBuilder.register(conversion)
   }

   public fun build(): Mikrom = Mikrom(rowMappers, parameterMappers, defaultConverters() + convertersBuilder.build(), namingStrategy)
}
