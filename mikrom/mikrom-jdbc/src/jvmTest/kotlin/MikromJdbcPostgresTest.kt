package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.queryFor
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer

class MikromJdbcPostgresTest : FunSpec(
   {
      val dbContainer = PostgreSQLContainer("postgres:16-alpine")
      val postgres = install(JdbcDatabaseContainerExtension(dbContainer))
      val dataSource = JdbcDataSource(postgres)

      val mikrom = Mikrom {
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

      dataSource.transaction {
         mikrom.execute(
            Query(
               """
                  CREATE TABLE books (
                     author VARCHAR(255),
                     title VARCHAR(255),
                     number_of_pages INT
                  )
               """.trimIndent(),
            ),
         )
      }

      afterEach {
         dataSource.transaction {
            mikrom.execute(Query("TRUNCATE TABLE books"))
         }
      }

      test("Reading data outside of transaction") {
         dataSource.transaction {
            mikrom.execute(
                Query("INSERT INTO books (author, title, number_of_pages) VALUES (?, ?, ?)"),
                listOf("George Orwell", "1984", 328),
            )

            val books = mikrom.queryFor<Book>(
               Query("SELECT * FROM books WHERE author = ?"),
               listOf("JRR Tolkien"),
            )

            books.shouldContainExactly(Book("JRR Tolkien", "The Hobbit", 310))
         }

         val books = dataSource.transaction {
            mikrom.queryFor<Book>(
               Query("SELECT * FROM books WHERE author = ?"),
               listOf("JRR Tolkien"),
            )
         }

         books.shouldContainExactly(
            Book("JRR Tolkien", "The Hobbit", 310),
         )
      }

      test("Should not allow SQL injection") {
         dataSource.transaction {
             mikrom.execute(
                 Query("INSERT INTO books (author, title, number_of_pages) VALUES (?, ?, ?)"),
                 listOf("George Orwell", "1984", 328),
             )
         }

         dataSource.transaction {
            mikrom.queryFor<Book>(
               Query("SELECT * FROM books WHERE author = ?"),
               listOf("JRR Tolkien; TRUNCATE TABLE books;"),
            )
         }

         dataSource.transaction {
            mikrom.queryFor<Book>(Query("SELECT * FROM books")).shouldNotBeEmpty()
         }
      }

      test("Should work with Postgres") {
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

            Rollback
         }

         dataSource.transaction {
            mikrom.queryFor<Book>(Query("SELECT * FROM books")).shouldBeEmpty()
         }
      }
   },
) {
   private data class Book(val author: String, val title: String, val numberOfPages: Int)
}
