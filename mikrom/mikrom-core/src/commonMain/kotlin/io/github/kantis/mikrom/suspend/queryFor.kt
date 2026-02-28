package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.nonMappedPrimitives
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(query: Query): Flow<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query).map { it.singleValue() as T }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T> Mikrom.queryFor(
   query: Query,
   param: Any,
): Flow<T> = queryFor(query, listOf(param))

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   query: Query,
   params: List<Any>,
): Flow<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query, params).map { it.singleValue() as T }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query, params).map { rowMapper.mapRow(it, this@queryFor) }
}
