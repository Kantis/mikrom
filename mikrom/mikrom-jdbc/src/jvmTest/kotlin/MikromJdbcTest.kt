package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.h2.prepareH2Database
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class Book(val author: String, val title: String, val numberOfPages: Int)

class MikromJdbcTest : FunSpec(
   {
      test("integrate with H2 JDBC data source") {
         val mikrom =
            Mikrom {
               registerRowMapper { row, mikrom ->
                  with(mikrom) {
                     Book(
                        row.get("author"),
                        row.get("title"),
                        row.get("number_of_pages"),
                     )
                  }
               }
            }

         val dataSource = prepareH2Database(
            """
               CREATE TABLE books (
                  author VARCHAR(255),
                  title VARCHAR(255),
                  number_of_pages INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO books (author, title, number_of_pages) VALUES (?, ?, ?)"),
               listOf("George Orwell", "1984", 328),
            )

            mikrom.queryFor<Book>(Query("SELECT * FROM books")) shouldBe
               listOf(
                  Book("JRR Tolkien", "The Hobbit", 310),
                  Book("George Orwell", "1984", 328),
               )

            mikrom.queryFor<Book>(Query("SELECT * FROM books WHERE number_of_pages > ?"), 320) shouldBe
               listOf(Book("George Orwell", "1984", 328))
         }
      }
   },
)
