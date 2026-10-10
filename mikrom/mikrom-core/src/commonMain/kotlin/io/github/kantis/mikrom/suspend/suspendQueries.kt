package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.ParsedQuery
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.parseNamedParameters
import io.github.kantis.mikrom.resolveQueryParams
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
) {
   transaction.executeInTransaction(query, emptyList<Any>())
}

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: List<Any?>,
) {
   transaction.executeInTransaction(query, params)
}

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: List<Any?>,
) {
   transaction.executeInTransaction(query, *params)
}

/**
 * Each element of [params] is resolved the same way as in the blocking API: a `List` is bound positionally, a
 * `Map<String, Any?>` resolves `:named` parameters, and any other object is mapped through its registered or
 * compiled [ParameterMapper].
 */
context(transaction: SuspendingTransaction)
public suspend fun <T : Any> Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: T,
) {
   val parsed = parseNamedParameters(query, transaction::placeholder)
   params.forEach {
      val resolved = resolveQueryParams(parsed, it)
      transaction.executeInTransaction(resolved.sql, resolved.params)
   }
}

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query, transaction::placeholder)
   when (parsed) {
      is ParsedQuery.Named -> execute(parsed.sql, parsed.resolveParams(params))
      is ParsedQuery.Positional -> execute(parsed.sql, listOf(params))
   }
}

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
   vararg paramMaps: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query, transaction::placeholder)
   when (parsed) {
      is ParsedQuery.Named -> paramMaps.forEach { execute(parsed.sql, parsed.resolveParams(it)) }
      is ParsedQuery.Positional -> paramMaps.forEach { execute(parsed.sql, listOf(it)) }
   }
}

/**
 * Executes [query] once per parameter set emitted by [params], and returns once the flow has completed.
 * Lets big inserts/updates stream their positional parameters instead of materializing them up front.
 */
context(transaction: SuspendingTransaction)
public suspend fun Mikrom.executeStreaming(
   @Language("SQL") query: Query,
   params: Flow<List<Any?>>,
) {
   transaction.executeInTransaction(query, params)
}
