package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.R2dbcTestDialect
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.github.kantis.mikrom.suspend.queryForFlow
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

fun queryingForPrimitivesTests(
   dialect: R2dbcTestDialect,
   dataSourceProvider: suspend () -> PooledR2dbcDataSource,
) = funSpec {
   val mikrom = Mikrom {}

   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = dataSourceProvider()
   }

   beforeEach {
      dataSource.suspendingTransaction {
         mikrom.execute(dialect.truncateTable("test_records"))
      }
   }

   test("[${dialect.name}] queryFor Long") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         mikrom.queryFor<Long>("SELECT COUNT(*) FROM test_records") shouldBe listOf(2L)
      }
   }

   test("[${dialect.name}] queryFor Int") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         mikrom.queryFor<Int>("SELECT COUNT(*) FROM test_records") shouldBe listOf(2)
      }
   }

   test("[${dialect.name}] queryFor String") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         mikrom.queryFor<String>("SELECT name FROM test_records ORDER BY name") shouldBe listOf("baz", "qux")
      }
   }

   test("[${dialect.name}] queryForFlow String") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("baz"),
            listOf("qux"),
         )

         mikrom.queryForFlow<String>("SELECT name FROM test_records ORDER BY name").toList() shouldBe listOf("baz", "qux")
      }
   }
}
