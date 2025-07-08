package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlin.collections.emptyList

context(SuspendingTransaction)
public suspend inline fun Mikrom.execute(
   query: Query,
   params: List<Any>,
) {
   executeInTransaction(query, params)
}

context(SuspendingTransaction)
public suspend fun <T : Any> Mikrom.execute(
   query: Query,
   vararg params: T,
) {
   params.forEach {
      if (it is List<*>)
         executeInTransaction(query, it)
      else
         executeInTransaction(query, listOf(it))
   }
}

context(SuspendingTransaction)
public suspend fun Mikrom.execute(query: Query) {
   executeInTransaction(query, emptyList<Any>())
}

/**
 * Allows streaming parameters for big inserts/updates.
 * Returns a [Job] that can be used to cancel the operation or await its completion.
 */
context(SuspendingTransaction)
public suspend fun Mikrom.executeStreaming(
   query: Query,
   params: Flow<List<Any>>,
): Job = executeInTransaction(query, params)
