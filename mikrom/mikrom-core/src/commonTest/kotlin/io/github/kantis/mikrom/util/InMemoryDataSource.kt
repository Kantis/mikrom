package io.github.kantis.mikrom.util

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.datasource.DataSource
import io.github.kantis.mikrom.datasource.Transaction

class InMemoryDataSource(
   val rows: List<Row>,
) : DataSource {
   override fun <T> transaction(block: Transaction.() -> T): T {
      val transaction = InMemoryTransaction(rows)

      return transaction.block()
   }

   private class InMemoryTransaction(val rows: List<Row>) : Transaction {
      override fun executeInTransaction(
         query: Query,
         vararg params: List<*>,
      ) {
         println("Received query $query")
      }

      override fun query(
         query: Query,
         params: List<*>,
      ): List<Row> = rows
   }
}
