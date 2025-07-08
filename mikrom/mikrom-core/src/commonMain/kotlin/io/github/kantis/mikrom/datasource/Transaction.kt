package io.github.kantis.mikrom.datasource

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row

public interface Transaction {
   public fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   )

   public fun query(
      query: Query,
      params: List<*> = emptyList<Any>(),
   ): List<Row>
}
