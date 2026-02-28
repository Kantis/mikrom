package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.r2dbc.helpers.preparePostgresDatabase
import io.github.kantis.mikrom.suspend.executeStreaming
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

private data class Book(val author: String, val title: String, val numberOfPages: Int)

class MikromR2dbcTest : FunSpec(
   {
      test("integrate with R2DBC PostgreSQL data source") {
         val mikrom = Mikrom {
            registerRowMapper { row ->
               Book(
                  row.get("author"),
                  row.get("title"),
                  row.get("number_of_pages"),
               )
            }
         }

         val dataSource = preparePostgresDatabase(
            """
                CREATE TABLE books (
                    author VARCHAR(255),
                    title VARCHAR(255),
                    number_of_pages INT
                )
            """.trimIndent(),
         )

         dataSource.suspendingTransaction {
            mikrom.executeStreaming(
               Query("INSERT INTO books (author, title, number_of_pages) VALUES ($1, $2, $3)"),
               flowOf(
                  listOf("JRR Tolkien", "The Hobbit", 310),
                  listOf("George Orwell", "1984", 328),
               ),
            ).join()

            mikrom
               .queryFor<Book>(Query("SELECT * FROM books ORDER BY author ASC"))
               .toList()
               .shouldContainExactly(
                  Book("George Orwell", "1984", 328),
                  Book("JRR Tolkien", "The Hobbit", 310),
               )

            mikrom.queryFor<Book>(Query("SELECT * FROM books WHERE number_of_pages > $1"), listOf(320)).toList() shouldBe
               listOf(Book("George Orwell", "1984", 328))
         }
      }
   },
)
