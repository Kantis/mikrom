package io.github.kantis.mikrom.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.testcontainers.containers.PostgreSQLContainer

class BookTest : FunSpec(
   {
      val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
         .apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
            start()
         }

      val hikariDataSource = HikariDataSource(
         HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            isAutoCommit = false
         },
      )

      val dataSource = JdbcDataSource(hikariDataSource)
      val mikrom = Mikrom { }
      val repository = BookRepository(dataSource, mikrom)

      beforeSpec {
         hikariDataSource.connection.use { conn ->
            conn.autoCommit = true
            conn.createStatement().use { stmt ->
               stmt.execute(
                  """
                  CREATE TABLE books (
                     id BIGSERIAL PRIMARY KEY,
                     author VARCHAR(255),
                     title VARCHAR(255),
                     number_of_pages INT
                  )
                  """.trimIndent(),
               )
            }
         }
      }

      afterSpec {
         hikariDataSource.close()
         postgres.stop()
      }

      beforeEach {
         dataSource.transaction {
            mikrom.execute("TRUNCATE TABLE books")
         }
      }

      test("create inserts a book and returns it with generated id") {
         val command = CreateBookCommand("JRR Tolkien", "The Fellowship of Mikrom", 201)

         val book = repository.create(command)

         book.author shouldBe "JRR Tolkien"
         book.title shouldBe "The Fellowship of Mikrom"
         book.numberOfPages shouldBe 201
         book.id shouldNotBe null
      }

      test("findById returns the created book") {
         val created = repository.create(CreateBookCommand("Terry Pratchett", "Going Postal", 480))

         val found = repository.findById(created.id)

         found shouldBe created
      }

      test("findById returns null for non-existent id") {
         val found = repository.findById(BookId(999L))

         found shouldBe null
      }
   },
)
