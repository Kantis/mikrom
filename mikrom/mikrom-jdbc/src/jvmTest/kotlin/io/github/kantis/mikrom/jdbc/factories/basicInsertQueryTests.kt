package io.github.kantis.mikrom.jdbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.JdbcTestDialect
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

private data class Book(val author: String, val title: String, val numberOfPages: Int)

fun basicInsertQueryTests(
   dialect: JdbcTestDialect,
   dataSourceProvider: () -> JdbcDataSource,
) = funSpec {
   val mikrom = Mikrom {
      registerRowMapper { row ->
         Book(
            row.get("author"),
            row.get("title"),
            row.get("number_of_pages"),
         )
      }
   }

   beforeEach {
      dataSourceProvider().transaction {
         mikrom.execute(dialect.truncateTable("books"))
      }
   }

   test("[${dialect.name}] insert and query books") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertBooks(),
            listOf("JRR Tolkien", "The Hobbit", 310),
            listOf("George Orwell", "1984", 328),
         )

         mikrom.queryFor<Book>("SELECT * FROM books ORDER BY author ASC")
            .shouldContainExactly(
               Book("George Orwell", "1984", 328),
               Book("JRR Tolkien", "The Hobbit", 310),
            )
      }
   }

   test("[${dialect.name}] query with parameter filter") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertBooks(),
            listOf("JRR Tolkien", "The Hobbit", 310),
            listOf("George Orwell", "1984", 328),
         )

         mikrom.queryFor<Book>(
            "SELECT * FROM books WHERE number_of_pages > ?",
            listOf(320),
         ) shouldBe listOf(Book("George Orwell", "1984", 328))
      }
   }

   test("[${dialect.name}] parameterized queries prevent SQL injection") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertBooks(),
            listOf("JRR Tolkien", "The Hobbit", 310),
         )
      }

      dataSourceProvider().transaction {
         mikrom.queryFor<Book>(
            "SELECT * FROM books WHERE author = ?",
            listOf("JRR Tolkien'; DROP TABLE books;--"),
         )
      }

      dataSourceProvider().transaction {
         mikrom.queryFor<Book>("SELECT * FROM books").shouldNotBeEmpty()
      }
   }
}
