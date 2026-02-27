package io.github.kantis.mikrom

import io.github.kantis.mikrom.util.InMemoryDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MikromTest : FunSpec({
   data class Foo(val bar: String)

   test("Resolve mapper") {
      val mikrom =
         Mikrom {
            registerRowMapper { row -> Foo(row.get("bar")) }
         }

      mikrom
         .resolveRowMapper<Foo>()
         .mapRow(Row.of("bar" to "baz")) shouldBe Foo("baz")
   }

   test("integrate with data sources") {
      val mikrom =
         Mikrom {
            registerRowMapper { row -> Foo(row.get("bar")) }
         }

      val dataSource =
         InMemoryDataSource(
            listOf(
               Row.of("bar" to "baz"),
               Row.of("bar" to "qux"),
            ),
         )

      dataSource.transaction {
         mikrom.queryFor<Foo>(Query("SELECT * FROM foo")) shouldBe
            listOf(
               Foo("baz"),
               Foo("qux"),
            )
      }
   }
})
