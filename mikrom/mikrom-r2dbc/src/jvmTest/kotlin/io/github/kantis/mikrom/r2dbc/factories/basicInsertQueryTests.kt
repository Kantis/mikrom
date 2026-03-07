package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.R2dbcTestDialect
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

private data class Book(val author: String, val title: String, val numberOfPages: Int)

fun basicInsertQueryTests(
   dialect: R2dbcTestDialect,
   streaming: Boolean,
   dataSourceProvider: suspend () -> PooledR2dbcDataSource,
) = funSpec {
   val mikrom = Mikrom.Companion {
      registerRowMapper { row ->
         Book(
            row.get("author"),
            row.get("title"),
            row.get("number_of_pages"),
         )
      }
   }

   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = dataSourceProvider()
   }

   beforeEach {
      dataSource.suspendingTransaction {
         mikrom.execute(dialect.truncateTable("books"))
      }
   }

   test("[${dialect.name}] ${streamingTestName(streaming)} insert and query books") {
      dataSource.suspendingTransaction {
         mikrom.testExecute(
            dialect.insertBooks(),
            streaming,
            listOf("JRR Tolkien", "The Hobbit", 310),
            listOf("George Orwell", "1984", 328),
         )

         mikrom
            .queryFor<Book>("SELECT * FROM books ORDER BY author ASC")
            .toList()
            .shouldContainExactly(
               Book("George Orwell", "1984", 328),
               Book("JRR Tolkien", "The Hobbit", 310),
            )
      }
   }

   test("[${dialect.name}] ${streamingTestName(streaming)} query with parameter filter") {
      dataSource.suspendingTransaction {
         mikrom.testExecute(
            dialect.insertBooks(),
            streaming,
            listOf("JRR Tolkien", "The Hobbit", 310),
            listOf("George Orwell", "1984", 328),
         )

         mikrom.queryFor<Book>(
            "SELECT * FROM books WHERE number_of_pages > ${dialect.placeholder(1)}",
            listOf(320),
         ).toList() shouldBe listOf(Book("George Orwell", "1984", 328))
      }
   }
}
