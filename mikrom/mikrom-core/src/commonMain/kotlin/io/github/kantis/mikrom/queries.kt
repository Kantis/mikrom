package io.github.kantis.mikrom

import io.github.kantis.mikrom.datasource.Transaction
import kotlin.collections.emptyList

public inline fun <reified T> Mikrom.queryForSingleOrNull(query: Query): T? = null

context(Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(query: Query): List<T> {
   if (T::class in nonMappedPrimitives) {
      return query(query).map { it.singleValue() as T }
   }
   val rowMapper = resolveRowMapper<T>()
   return query(query).map(rowMapper::mapRow)
}

context(Transaction)
public inline fun <reified T> Mikrom.queryFor(
   query: Query,
   param: Any,
): List<T> = queryFor(query, listOf(param))

context(Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   query: Query,
   params: List<Any>,
): List<T> {
   if (T::class in nonMappedPrimitives) {
      return query(query, params).map { it.singleValue() as T }
   }
   val rowMapper = resolveRowMapper<T>()
   return query(query, params).map(rowMapper::mapRow)
}

context(Transaction)
public inline fun Mikrom.execute(
   query: Query,
   params: List<Any>,
) {
   executeInTransaction(query, params)
}

context(Transaction)
public fun Mikrom.execute(
   query: Query,
   vararg params: List<Any>,
) {
   params.forEach { executeInTransaction(query, it) }
}

context(Transaction)
public fun <T : Any> Mikrom.execute(
   query: Query,
   vararg params: T,
) {
   params.forEach { if (it is List<*>) executeInTransaction(query, it) else executeInTransaction(query, listOf(it)) }
}

context(Transaction)
public fun Mikrom.execute(query: Query) {
   executeInTransaction(query, emptyList<Any>())
}
