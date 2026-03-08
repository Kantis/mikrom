package io.github.kantis.mikrom

import io.github.kantis.mikrom.datasource.Transaction
import io.github.kantis.mikrom.internal.compiledParameterMapper
import org.intellij.lang.annotations.Language
import kotlin.collections.emptyList

public inline fun <reified T> Mikrom.queryForSingleOrNull(
   @Language("SQL") query: Query,
): T? = null

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
): List<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query).map { it.convertSingleValue(T::class, converters) }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: Transaction)
public inline fun <reified T> Mikrom.queryFor(
   @Language("SQL") query: Query,
   param: Any,
): List<T> = queryFor(query, listOf(param))

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: List<Any?>,
): List<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query, params).map { it.convertSingleValue(T::class, converters) }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query, params).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: List<Any?>,
) {
   transaction.executeInTransaction(query, params)
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: List<Any?>,
) {
   params.forEach { transaction.executeInTransaction(query, it) }
}

@Suppress("UNCHECKED_CAST")
context(transaction: Transaction)
public fun <T : Any> Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: T,
) {
   params.forEach {
      val mapper = parameterMappers[it::class] as? ParameterMapper<T>
         ?: it::class.compiledParameterMapper() as? ParameterMapper<T>
      if (mapper != null) {
         execute(query, mapper.mapParameters(it))
      } else if (it is List<*>) {
         transaction.executeInTransaction(query, it)
      } else {
         transaction.executeInTransaction(query, listOf(it))
      }
   }
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
) {
   transaction.executeInTransaction(query, emptyList<Any>())
}

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: Map<String, Any?>,
): List<T> {
   val parsed = parseNamedParameters(query)
   return queryFor(parsed.sql, parsed.resolveParams(params))
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query)
   execute(parsed.sql, parsed.resolveParams(params))
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   vararg paramMaps: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query)
   paramMaps.forEach {
      execute(parsed.sql, parsed.resolveParams(it))
   }
}
