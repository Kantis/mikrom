package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.jdbc.h2.prepareH2Database
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class QueryingForPrimitivesTest : FunSpec({
   val dataSource = prepareH2Database(
      """
         CREATE TABLE foo(
           id INT PRIMARY KEY AUTO_INCREMENT,
           bar VARCHAR(255)
         );
      """.trimIndent(),
   )

   val mikrom = Mikrom { }

   beforeEach {
      dataSource.transaction {
         mikrom.execute("TRUNCATE TABLE foo")
      }
   }

   test("Int values should work") {
      dataSource.transaction {
         mikrom.execute(
            "INSERT INTO foo (bar) VALUES (?)",
            listOf("baz"),
            listOf("qux"),
         )

         val result = mikrom.queryFor<Int>("SELECT id FROM foo")

         result shouldBe listOf(1, 2)
      }
   }

   test("String values should work") {
      dataSource.transaction {
         mikrom.execute(
            "INSERT INTO foo (bar) VALUES (?)",
            listOf("baz"),
            listOf("qux"),
         )

         val result = mikrom.queryFor<String>("SELECT bar FROM foo")

         result shouldBe listOf("baz", "qux")
      }
   }
})
