package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.nonMappedPrimitives
import io.github.kantis.mikrom.parseNamedParameters
import io.github.kantis.mikrom.resolveQueryParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.intellij.lang.annotations.Language

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
): Flow<T> = mapRowFlow<T>(transaction.query(query))

/**
 * [params] is resolved the same way as in the blocking API: a `List` is bound positionally, a `Map<String, Any?>`
 * resolves `:named` parameters, and any other object is mapped through its registered or compiled [ParameterMapper].
 */
context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: Any,
): Flow<T> {
   val resolved = resolveQueryParams(parseNamedParameters(query, transaction::placeholder), params)
   return mapRowFlow<T>(transaction.query(resolved.sql, resolved.params))
}

@PublishedApi
internal inline fun <reified T : Any> Mikrom.mapRowFlow(rows: Flow<Row>): Flow<T> =
   if (T::class in nonMappedPrimitives) {
      rows.map { it.convertSingleValue(T::class, converters) }
   } else {
      val rowMapper = resolveRowMapper<T>()
      rows.map { rowMapper.mapRow(it, this) }
   }
