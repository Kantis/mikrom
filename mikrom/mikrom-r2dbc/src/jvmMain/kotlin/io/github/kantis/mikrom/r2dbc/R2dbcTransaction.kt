package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.suspend.SuspendingTransaction
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlin.coroutines.CoroutineContext

public class R2dbcTransaction(private val connection: Connection, override val coroutineContext: CoroutineContext) : SuspendingTransaction {
   override suspend fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   ) {
      val statement = connection.createStatement(query.value)
      params.forEach { p ->
         println("Executing query: ${query.value} with params: $p")
         bindParameters(statement, p)
         statement.execute {
            println("executeInTransaction returned result: $it")
         }
      }
   }

   // This is probably a pretty niche use-case, streaming data into the DB... should probably just use a list of params..
   override suspend fun executeInTransaction(
      query: Query,
      params: Flow<List<*>>,
   ): Job =
      launch {
         val statement = connection.createStatement(query.value)
         params.collect { p ->
            println("Executing query: ${query.value} with params: $p")
            bindParameters(statement, p)
            statement.execute {
               println("executeInTransaction returned result: $it")
            }
         }
      }

   override suspend fun query(
      query: Query,
      params: List<*>,
   ): Flow<Row> =
      flow {
         val statement = connection.createStatement(query.value)
         bindParameters(statement, params)
         statement.execute { emit(it) }
      }

   private suspend fun Statement.execute(collector: FlowCollector<Row>) {
      execute()
         .asFlow()
         .collect { result ->
            result.map { row, rowMetadata ->
               rowMetadata.columnMetadatas.associate { metadata ->
                  metadata.name to row.get(metadata.name)
               }
            }.asFlow().collect(collector)
         }
   }

   private fun bindParameters(
      statement: Statement,
      params: List<*>,
   ) {
      params.forEachIndexed { index, param ->
         if (param == null) {
            // TODO: How can we know the type of the column?
            statement.bindNull(index, Object::class.java)
         } else {
            statement.bind(index, param)
         }
      }
   }
}
