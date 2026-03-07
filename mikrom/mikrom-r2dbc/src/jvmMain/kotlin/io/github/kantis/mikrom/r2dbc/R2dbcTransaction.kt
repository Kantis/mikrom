package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.buildRow
import io.github.kantis.mikrom.suspend.SuspendingTransaction
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
public class R2dbcTransaction(private val connection: Connection, override val coroutineContext: CoroutineContext) : SuspendingTransaction {
   override suspend fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   ) {
      params.forEach { p ->
         val statement = connection.createStatement(query)
         println("Executing query: $query with params: $p")
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
         params.collect { p ->
            val statement = connection.createStatement(query)
            println("Executing query: $query with params: $p")
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
         val statement = connection.createStatement(query)
         bindParameters(statement, params)
         statement.execute { emit(it) }
      }

   private suspend fun Statement.execute(collector: FlowCollector<Row>) {
      execute()
         .asFlow()
         .flatMapConcat { result ->
            result.map { row, rowMetadata ->
               buildRow {
                  for (metadata in rowMetadata.columnMetadatas) {
                     column(
                        name = metadata.name.lowercase(),
                        value = row.get(metadata.name),
                        sqlTypeName = metadata.type.name,
                     )
                  }
               }
            }.asFlow()
         }.collect(collector)
   }

   private fun bindParameters(
      statement: Statement,
      params: List<*>,
   ) {
      params.forEachIndexed { index, param ->
         when (param) {
            is TypedNull -> statement.bindNull(index, param.type.java)
            null -> statement.bindNull(index, Any::class.java)
            else -> statement.bind(index, param)
         }
      }
   }
}
