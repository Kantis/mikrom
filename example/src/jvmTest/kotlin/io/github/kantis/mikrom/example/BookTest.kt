package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypeConversions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BookTest : FunSpec(
   {
      val mikrom = Mikrom(mutableMapOf(), TypeConversions.EMPTY)

      test("rowMapper() companion accessor returns generated mapper") {
         val book: Book = Book.rowMapper().mapRow(
            Row.of(
               "author" to "JRR Tolkien",
               "title" to "The Fellowship of Mikrom",
               "numberOfPages" to 201,
            ),
            mikrom,
         )

         book shouldBe Book("JRR Tolkien", "The Fellowship of Mikrom", 201)
      }

      test("resolveRowMapper discovers compiled mapper without explicit registration") {
         val mapper = mikrom.resolveRowMapper<Book>()
         val book = mapper.mapRow(
            Row.of(
               "author" to "Terry Pratchett",
               "title" to "Going Postal",
               "numberOfPages" to 480,
            ),
            mikrom,
         )

         book shouldBe Book("Terry Pratchett", "Going Postal", 480)
      }
   },
)
