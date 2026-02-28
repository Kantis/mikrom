package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.r2dbc.helpers.preparePostgresDatabase
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

private data class TestRecord(val id: Int, val name: String)

class R2dbcTransactionTest : FunSpec(
   {
      val mikrom = Mikrom {
         registerRowMapper { row, mikrom ->
            with(mikrom) {
               TestRecord(
                  row.get("id"),
                  row.get("name"),
               )
            }
         }
      }

      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = preparePostgresDatabase(
            """
                CREATE TABLE test_records (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255)
                )
            """.trimIndent(),
         )
      }

      beforeEach {
         dataSource.suspendingTransaction {
            mikrom.execute(Query("TRUNCATE TABLE test_records"))
         }
      }

      test("transaction commits successfully") {
         dataSource.suspendingTransaction {
            mikrom.execute(
               Query("INSERT INTO test_records (name) VALUES ($1)"),
               listOf("test record"),
            )
         }

         // Verify data persisted after transaction
         dataSource.suspendingTransaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records")).toList()
            records.size shouldBe 1
            records[0].name shouldBe "test record"
         }
      }

      test("transaction rolls back on explicit rollback") {
         val result = dataSource.suspendingTransaction {
            mikrom.execute(
               Query("INSERT INTO test_records (name) VALUES ($1)"),
               listOf("record to rollback"),
            )

            Rollback
         }

         result shouldBe Rollback

         // Verify data was not persisted
         dataSource.suspendingTransaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records")).toList()
            records.shouldContainExactly()
         }
      }

      test("transaction rolls back on exception") {
         try {
            dataSource.suspendingTransaction {
               mikrom.execute(
                  Query("INSERT INTO test_records (name) VALUES ($1)"),
                  listOf("test record"),
               )

               throw RuntimeException("Test exception")
            }
         } catch (e: RuntimeException) {
            e.message shouldBe "Test exception"
         }

         // Verify data was not persisted due to rollback
         dataSource.suspendingTransaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records")).toList()
            records.size shouldBe 0
         }
      }
   },
)
