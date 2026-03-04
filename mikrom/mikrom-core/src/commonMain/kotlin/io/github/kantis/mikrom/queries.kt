package io.github.kantis.mikrom

import io.github.kantis.mikrom.datasource.Transaction
import kotlin.collections.emptyList

public inline fun <reified T> Mikrom.queryForSingleOrNull(query: Query): T? = null

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(query: Query): List<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query).map { it.singleValue() as T }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: Transaction)
public inline fun <reified T> Mikrom.queryFor(
   query: Query,
   param: Any,
): List<T> = queryFor(query, listOf(param))

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   query: Query,
   params: List<Any>,
): List<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query, params).map { it.singleValue() as T }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query, params).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: Transaction)
public fun Mikrom.execute(
   query: Query,
   params: List<Any>,
) {
   transaction.executeInTransaction(query, params)
}

context(transaction: Transaction)
public fun Mikrom.execute(
   query: Query,
   vararg params: List<Any>,
) {
   params.forEach { transaction.executeInTransaction(query, it) }
}

context(transaction: Transaction)
public fun <T : Any> Mikrom.execute(
   query: Query,
   vararg params: T,
) {
   params.forEach {
      if (it is List<*>) transaction.executeInTransaction(query, it) else transaction.executeInTransaction(query, listOf(it))
   }
}

context(transaction: Transaction)
public fun Mikrom.execute(query: Query) {
   transaction.executeInTransaction(query, emptyList<Any>())
}

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   query: Query,
   params: Map<String, Any?>,
): List<T> {
   val parsed = parseNamedParameters(query.value)
   return queryFor(Query(parsed.sql), parsed.resolveParams(params).filterNotNull())
}

context(transaction: Transaction)
public fun Mikrom.execute(
   query: Query,
   params: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query.value)
   execute(Query(parsed.sql), parsed.resolveParams(params).filterNotNull())
}

context(transaction: Transaction)
public fun Mikrom.execute(
   query: Query,
   vararg paramMaps: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query.value)
   val parsedQuery = Query(parsed.sql)
   paramMaps.forEach {
      execute(parsedQuery, parsed.resolveParams(it).filterNotNull())
   }
}
