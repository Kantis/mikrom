package io.github.kantis.mikrom.datasource

import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import org.intellij.lang.annotations.Language

public interface Transaction {
   public fun executeInTransaction(
      @Language("SQL") query: Query,
      vararg params: List<*>,
   )

   public fun query(
      @Language("SQL") query: Query,
      params: List<*> = emptyList<Any>(),
   ): List<Row>
}
