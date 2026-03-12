package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.datasource.DataSource
import io.github.kantis.mikrom.queryFor

public class BookRepository(
   private val dataSource: DataSource,
   private val mikrom: Mikrom,
) {
   public fun create(command: CreateBookCommand): Book =
      dataSource.transaction {
         mikrom.queryFor<Book>(
            """
               INSERT INTO books (author, title, number_of_pages)
               VALUES (:author, :title, :numberOfPages)
               RETURNING books.*
            """.trimIndent(),
            command,
         ).single()
      }

   public fun findById(id: BookId): Book? =
      dataSource.transaction {
         mikrom.queryFor<Book>(
            "SELECT * FROM books WHERE id = ?",
            id.value,
         ).singleOrNull()
      }
}
