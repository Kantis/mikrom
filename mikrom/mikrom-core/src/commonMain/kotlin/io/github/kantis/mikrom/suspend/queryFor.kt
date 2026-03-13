package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.ParsedQuery
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.nonMappedPrimitives
import io.github.kantis.mikrom.parseNamedParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.intellij.lang.annotations.Language

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
): Flow<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query).map { it.convertSingleValue(T::class, converters) }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T> Mikrom.queryFor(
   @Language("SQL") query: Query,
   param: Any,
): Flow<T> = queryFor(query, listOf(param))

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: List<Any>,
): Flow<T> {
   if (T::class in nonMappedPrimitives) {
      return transaction.query(query, params).map { it.convertSingleValue(T::class, converters) }
   }
   val rowMapper = resolveRowMapper<T>()
   return transaction.query(query, params).map { rowMapper.mapRow(it, this@queryFor) }
}

context(transaction: SuspendingTransaction)
public suspend inline fun <reified T : Any> Mikrom.queryFor(
   @Language("SQL") query: Query,
   params: Map<String, Any>,
): Flow<T> {
   val parsed = parseNamedParameters(query)
   return when (parsed) {
      is ParsedQuery.Named -> queryFor(parsed.sql, parsed.resolveParams(params))
      is ParsedQuery.Positional -> queryFor(parsed.sql, listOf(params))
   }
}
