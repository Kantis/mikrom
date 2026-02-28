package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypeConversions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BookTest : FunSpec(
   {
      test("Foo") {
         val mikrom = Mikrom(mutableMapOf(), TypeConversions.EMPTY)
         val book: Book = Book.RowMapper.mapRow(
            Row.of(
               "author" to "JRR Tolkien",
               "title" to "The Fellowship of Mikrom",
               "numberOfPages" to 201,
            ),
            mikrom,
         )

         book shouldBe Book("JRR Tolkien", "The Fellowship of Foo", 201)
      }
   },
)
