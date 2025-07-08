package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

public interface SuspendingTransaction : CoroutineScope {
   /**
    * Executes a query with the given parameters in a transaction.
    */
   public suspend fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   )

   /**
    * Allows streaming parameters for big inserts/updates.
    * Returns a [Job] that can be used to cancel the operation or await its completion.
    */
   public suspend fun executeInTransaction(
      query: Query,
      params: Flow<List<*>>,
   ): Job

   /**
    * Execute a query and stream the results as a [Flow] of [Row]. Mikrom will translate the Row's to the appropriate type
    * when using the [queryFor] function.
    */
   public suspend fun query(
      query: Query,
      params: List<*> = emptyList<Any>(),
   ): Flow<Row>
}
