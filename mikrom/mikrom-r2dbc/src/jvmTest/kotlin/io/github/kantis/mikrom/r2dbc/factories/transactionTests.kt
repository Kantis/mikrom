package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.R2dbcTestDialect
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

private data class TestRecord(val id: Int, val name: String)

fun transactionTests(
   dialect: R2dbcTestDialect,
   streaming: Boolean,
   dataSourceProvider: suspend () -> PooledR2dbcDataSource,
) = funSpec {
   val mikrom = Mikrom.Companion {
      registerRowMapper { row ->
         TestRecord(
            row.get("id"),
            row.get("name"),
         )
      }
   }

   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = dataSourceProvider()
   }

   beforeEach {
      dataSource.suspendingTransaction {
         mikrom.execute(dialect.truncateTable("test_records"))
      }
   }

   test("[${dialect.name}] ${streamingTestName(streaming)} transaction commits successfully") {
      dataSource.suspendingTransaction {
         mikrom.testExecute(
            dialect.insertTestRecord(),
            streaming,
            listOf("test record"),
         )
      }

      dataSource.suspendingTransaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records").toList()
         records.size shouldBe 1
         records[0].name shouldBe "test record"
      }
   }

   test("[${dialect.name}] ${streamingTestName(streaming)} transaction rolls back on explicit rollback") {
      val result = dataSource.suspendingTransaction {
         mikrom.testExecute(
            dialect.insertTestRecord(),
            streaming,
            listOf("record to rollback"),
         )

         Rollback
      }

      result shouldBe Rollback

      dataSource.suspendingTransaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records").toList()
         records.shouldContainExactly()
      }
   }

/*
   test("[${dialect.name}] ${streamingTestName(streaming)} transaction rolls back on exception") {
      try {
         dataSource.suspendingTransaction {
            mikrom.testExecute(
               dialect.insertTestRecord(),
               streaming,
               listOf("test record"),
            )

            throw RuntimeException("Test exception")
         }
      } catch (e: RuntimeException) {
         e.message shouldBe "Test exception"
      }

      dataSource.suspendingTransaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records").toList()
         records.size shouldBe 0
      }
   }
*/
}
