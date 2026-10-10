package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.AnsiString
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.buildRow
import io.github.kantis.mikrom.convert.TypeConverters
import io.github.kantis.mikrom.suspend.SuspendingTransaction
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow

@OptIn(ExperimentalCoroutinesApi::class)
public class R2dbcTransaction(
   private val connection: Connection,
   private val driverConverters: TypeConverters = TypeConverters.EMPTY,
   private val placeholderStyle: (Int) -> String = { "?" },
) : SuspendingTransaction {
   override fun placeholder(index: Int): String = placeholderStyle(index)

   /**
    * Binds every parameter set to a single statement, separated by [Statement.add], so the driver can batch them in
    * one round trip.
    */
   override suspend fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   ) {
      if (params.isEmpty()) return
      val statement = connection.createStatement(query)
      params.forEachIndexed { index, p ->
         if (index > 0) statement.add()
         bindParameters(statement, p)
      }
      statement.execute()
         .asFlow()
         .flatMapConcat { it.rowsUpdated.asFlow() }
         .collect()
   }

   override suspend fun executeInTransaction(
      query: Query,
      params: Flow<List<*>>,
   ) {
      params.collect { p -> executeInTransaction(query, p) }
   }

   override fun queryFlow(
      query: Query,
      params: List<*>,
   ): Flow<Row> =
      flow {
         val statement = connection.createStatement(query)
         bindParameters(statement, params)
         statement.execute()
            .asFlow()
            .flatMapConcat { result ->
               result.map { row, rowMetadata ->
                  buildRow(driverConverters) {
                     for (metadata in rowMetadata.columnMetadatas) {
                        column(
                           name = metadata.name.lowercase(),
                           value = row.get(metadata.name),
                           sqlTypeName = metadata.type.name,
                        )
                     }
                  }
               }.asFlow()
            }
            .collect { emit(it) }
      }

   private fun bindParameters(
      statement: Statement,
      params: List<*>,
   ) {
      params.forEachIndexed { index, param ->
         when (param) {
            is AnsiString -> statement.bind(index, param.value)
            is TypedNull -> statement.bindNull(index, param.type.javaObjectType)
            null -> throw IllegalArgumentException("Null value for parameter at index $index, must be passed using TypedNull!")
            else -> statement.bind(index, param)
         }
      }
   }
}
