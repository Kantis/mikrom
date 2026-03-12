package io.github.kantis.mikrom

import io.github.kantis.mikrom.datasource.Transaction
import io.github.kantis.mikrom.internal.compiledParameterMapper
import io.github.kantis.mikrom.internal.compiledRowMapper
import org.intellij.lang.annotations.Language

public inline fun <reified T> Mikrom.queryForSingleOrNull(
   @Language("SQL") query: Query,
): T? = null

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
): List<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query).map { it.convertSingleValue(T::class, conversions) }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query).map { rowMapper.mapRow(it, this@queryFor) }
}

@Suppress("UNCHECKED_CAST")
context(transaction: Transaction)
public inline fun <reified T : Any, reified P : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: P,
): List<T> {
   val parsed = parseNamedParameters(query)
   val parameterMapper: ParameterMapper<P>? = (parameterMappers[P::class] as? ParameterMapper<P>) ?: P::class.compiledParameterMapper()
   val rowMapper: RowMapper<T>? = (rowMappers[T::class] as? RowMapper<T>) ?: T::class.compiledRowMapper()

   val result =
      if (parameterMapper == null) {
         if (params is List<*>)
            transaction.query(query, params)
         else if (params is Map<*, *>)
            (params as? Map<String, Any?>)?.let { p ->
               transaction.query(parsed.sql, parsed.resolveParams(p))
            } ?: error("Received non-string based map as parameter")
         else
            transaction.query(parsed.sql, listOf(params))
      } else {
         if (params is Map<*, *>) {
            transaction.query(parsed.sql, listOf(params))
         } else {
            transaction.query(parsed.sql, parsed.resolveParams(parameterMapper.mapParameters(params)))
         }
      }

   return if (T::class in nonMappedPrimitives) {
      result.map { it.convertSingleValue(T::class, conversions) }
   } else {
      requireNotNull(rowMapper) { "No RowMapper found for ${T::class.simpleName}" }
      result.map { rowMapper.mapRow(it, this@queryFor) }
   }
}
