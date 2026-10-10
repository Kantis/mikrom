package io.github.kantis.mikrom.suspend

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

public interface SuspendingTransaction {
   /**
    * Executes [query] once per parameter set in [params].
    */
   public suspend fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   )

   /**
    * Executes [query] once per parameter set emitted by [params], and returns once the flow has completed.
    * Lets big inserts/updates stream their parameters instead of materializing them up front.
    */
   public suspend fun executeInTransaction(
      query: Query,
      params: Flow<List<*>>,
   )

   /**
    * Executes [query] and loads every resulting [Row]. Mikrom translates the rows to the requested type in [queryFor].
    */
   public suspend fun query(
      query: Query,
      params: List<*> = emptyList<Any>(),
   ): List<Row> = queryFlow(query, params).toList()

   /**
    * Executes [query] when the returned [Flow] is collected and streams the resulting [Row]s.
    * The flow uses this transaction's connection, so it must be collected before the transaction completes.
    */
   public fun queryFlow(
      query: Query,
      params: List<*> = emptyList<Any>(),
   ): Flow<Row>

   /**
    * The positional placeholder the underlying driver expects for the [index]:th (1-based) parameter.
    * Used when rewriting `:named` parameters into positional ones. Defaults to `?`.
    */
   public fun placeholder(index: Int): String = "?"
}
