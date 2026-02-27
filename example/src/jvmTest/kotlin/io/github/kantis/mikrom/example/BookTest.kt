package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.Row
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BookTest : FunSpec(
   {
      test("Foo") {
         val book: Book = Book.RowMapper.mapRow(
            Row.of(
               "author" to "JRR Tolkien",
               "title" to "The Fellowship of Mikrom",
               "numberOfPages" to 201,
            ),
         )

         book shouldBe Book("JRR Tolkien", "The Fellowship of Foo", 201)
      }
   },
)
