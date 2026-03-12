package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.ParsedQuery
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.parseNamedParameters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language

context(transaction: SuspendingTransaction)
public suspend inline fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: List<Any?>,
) {
   transaction.executeInTransaction(query, params)
}

context(transaction: SuspendingTransaction)
public suspend fun <T : Any> Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: T,
) {
   params.forEach {
      if (it is List<*>)
         transaction.executeInTransaction(query, it)
      else
         transaction.executeInTransaction(query, listOf(it))
   }
}

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
) {
   transaction.executeInTransaction(query, emptyList<Any>())
}

/**
 * Allows streaming parameters for big inserts/updates.
 * Returns a [Job] that can be used to cancel the operation or await its completion.
 */
context(transaction: SuspendingTransaction)
public suspend fun Mikrom.executeStreaming(
   @Language("SQL") query: Query,
   params: Flow<List<Any>>,
): Job = transaction.executeInTransaction(query, params)

context(transaction: SuspendingTransaction)
public suspend fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: Map<String, Any>,
) {
   val parsed = parseNamedParameters(query)
   when (parsed) {
      is ParsedQuery.Named -> execute(parsed.sql, parsed.resolveParams(params))
      is ParsedQuery.Positional -> execute(parsed.sql, listOf(params))
   }
}
