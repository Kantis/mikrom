package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.mapRows
import io.github.kantis.mikrom.nonMappedPrimitives
import io.github.kantis.mikrom.parseNamedParameters
import io.github.kantis.mikrom.resolveQueryParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.intellij.lang.annotations.Language

/**
 * Executes [query] and maps every row to [T]. The result is fully loaded, so it is safe to return from the
 * transaction block. Use [queryForFlow] to stream large results instead.
 */
context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
): List<T> = mapRows<T>(transaction.query(query))

/**
 * Executes [query] with [params] and maps every row to [T]. The result is fully loaded, so it is safe to return from
 * the transaction block. Use [queryForFlow] to stream large results instead.
 *
 * [params] is resolved the same way as in the blocking API: a `List` is bound positionally, a `Map<String, Any?>`
 * resolves `:named` parameters, and any other object is mapped through its registered or compiled [ParameterMapper].
 */
context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: Any,
): List<T> {
   val resolved = resolveQueryParams(parseNamedParameters(query, transaction::placeholder), params)
   return mapRows<T>(transaction.query(resolved.sql, resolved.params))
}

/**
 * Streams the rows of [query] as a cold [Flow] of [T]. The query runs when the flow is collected, on the
 * transaction's connection, so the flow must be collected before the transaction block returns.
 */
context(transaction: SuspendingTransaction)
public inline fun <reified T : Any> Mikrom.queryForFlow(
   @Language("SQL") query: Query,
): Flow<T> = mapRowFlow<T>(transaction.queryFlow(query))

/**
 * Streams the rows of [query] with [params] as a cold [Flow] of [T]. The query runs when the flow is collected, on the
 * transaction's connection, so the flow must be collected before the transaction block returns.
 *
 * [params] is resolved the same way as in [queryFor].
 */
context(transaction: SuspendingTransaction)
public inline fun <reified T : Any> Mikrom.queryForFlow(
   @Language("SQL") query: Query,
   params: Any,
): Flow<T> {
   val resolved = resolveQueryParams(parseNamedParameters(query, transaction::placeholder), params)
   return mapRowFlow<T>(transaction.queryFlow(resolved.sql, resolved.params))
}

@PublishedApi
internal inline fun <reified T : Any> Mikrom.mapRowFlow(rows: Flow<Row>): Flow<T> =
   if (T::class in nonMappedPrimitives) {
      rows.map { it.convertSingleValue(T::class, converters) }
   } else {
      val rowMapper = resolveRowMapper<T>()
      rows.map { rowMapper.mapRow(it, this) }
   }
