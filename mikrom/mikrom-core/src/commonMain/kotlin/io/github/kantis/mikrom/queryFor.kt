package io.github.kantis.mikrom

import io.github.kantis.mikrom.datasource.Transaction
import org.intellij.lang.annotations.Language

public inline fun <reified T> Mikrom.queryForSingleOrNull(
   @Language("SQL") query: Query,
): T? = null

context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
): List<T> = mapRows<T>(transaction.query(query))

@Suppress("UNCHECKED_CAST")
context(transaction: Transaction)
public inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: Any,
): List<T> {
   val resolved = resolveQueryParams(parseNamedParameters(query), params)
   return mapRows<T>(transaction.query(resolved.sql, resolved.params))
}

@PublishedApi
internal inline fun <reified T : Any> Mikrom.mapRows(rows: List<Row>): List<T> =
   if (T::class in nonMappedPrimitives) {
      rows.map { it.convertSingleValue(T::class, converters) }
   } else {
      val rowMapper = resolveRowMapper<T>()
      rows.map { rowMapper.mapRow(it, this) }
   }
