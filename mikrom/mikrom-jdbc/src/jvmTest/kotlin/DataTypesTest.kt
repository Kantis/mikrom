package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.getOrNull
import io.github.kantis.mikrom.jdbc.h2.prepareH2Database
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.Instant

class DataTypesTest : FunSpec({
   val dataSource = prepareH2Database(
      """
         CREATE TABLE foo(
           id INT PRIMARY KEY AUTO_INCREMENT,
           bar VARCHAR(255),
           number DECIMAL(10,2),
           timestamp TIMESTAMP
         );
      """.trimIndent(),
   )

   val mikrom = Mikrom { }

   test("Mixing null as a parameter should work") {
      val now = Instant.now()

      dataSource.transaction {
         mikrom.execute(
            Query("INSERT INTO foo (bar, number, timestamp) VALUES (?, ?, ?)"),
            listOf("baz", "123.01".toBigDecimal(), now),
            listOf(null, null, null),
         )

         val result = query(Query("SELECT * FROM foo"))

         result.size shouldBe 2
         result[0].get<Int>("id") shouldBe 1
         result[0].get<String>("bar") shouldBe "baz"
         result[0].get<BigDecimal>("number") shouldBe "123.01".toBigDecimal()
         result[0].get<Instant>("timestamp") shouldBeEqual now
         result[1].get<Int>("id") shouldBe 2
         result[1].getOrNull<String>("bar") shouldBe null
      }
   }
})
