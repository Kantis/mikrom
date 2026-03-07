package io.github.kantis.mikrom.jdbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.JdbcTestDialect
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe

fun queryingForPrimitivesTests(
   dialect: JdbcTestDialect,
   dataSourceProvider: () -> JdbcDataSource,
) = funSpec {
   val mikrom = Mikrom {}

   beforeEach {
      dataSourceProvider().transaction {
         mikrom.execute(dialect.truncateTable("test_records"))
      }
   }

   test("[${dialect.name}] queryFor Long") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         val result = mikrom.queryFor<Long>("SELECT COUNT(*) FROM test_records")
         result shouldBe listOf(2L)
      }
   }

   test("[${dialect.name}] queryFor Int") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         val result = mikrom.queryFor<Int>("SELECT COUNT(*) FROM test_records")
         result shouldBe listOf(2)
      }
   }

   test("[${dialect.name}] queryFor String") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         val result = mikrom.queryFor<String>("SELECT name FROM test_records ORDER BY name")
         result shouldBe listOf("baz", "qux")
      }
   }
}
