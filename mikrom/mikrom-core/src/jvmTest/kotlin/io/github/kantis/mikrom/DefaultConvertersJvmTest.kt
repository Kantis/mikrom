package io.github.kantis.mikrom

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

class DefaultConvertersJvmTest : FunSpec({
   val now = Instant.parse("2023-12-25T15:30:45.001Z")
   val timestamp = Timestamp.from(now)
   val mikrom = Mikrom(mutableMapOf())
   val row = Row.of("ts" to timestamp)

   test("Timestamp is converted to Instant") {
      with(mikrom) {
         row.get<Instant>("ts") shouldBe now
      }
   }

   test("Timestamp is converted to LocalDateTime") {
      with(mikrom) {
         row.get<LocalDateTime>("ts") shouldBe timestamp.toLocalDateTime()
      }
   }
})
