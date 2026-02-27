package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.h2.prepareH2Database
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

data class TestRecord(val id: Int, val name: String)

class JdbcTransactionTest : FunSpec(
   {
      val mikrom = Mikrom {
         registerRowMapper { row -> TestRecord(row.get("id"), row.get("name")) }
      }

      val dataSource = prepareH2Database(
         """
            CREATE TABLE test_records(
              id INT PRIMARY KEY,
              name VARCHAR(255)
            );
         """.trimIndent(),
      )

      afterEach {
         dataSource.transaction {
            mikrom.execute(Query("TRUNCATE TABLE test_records"))
         }
      }

      test("transaction commits data when returning TransactionResult.Commit") {
         // Insert data and commit
         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
               listOf(1, "committed_name"),
            )
         }

         // Verify data persists after commit
         dataSource.transaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records"))
            records shouldHaveSize 1
            records[0] shouldBe TestRecord(1, "committed_name")
         }
      }

      test("transaction rolls back data when returning TransactionResult.Rollback") {
         // Insert initial data and commit
         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
               listOf(1, "initial_name"),
            )
         }

         // Insert more data but rollback
         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
               listOf(2, "rolled_back_name"),
            )
            Rollback
         }

         // Verify only initial data remains
         dataSource.transaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records ORDER BY id"))
            records.shouldContainExactly(TestRecord(1, "initial_name"))
         }
      }

      test("transaction rolls back when exception is thrown") {
         // Insert initial data and commit
         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
               listOf(1, "initial_name"),
            )
         }

         // Attempt transaction that throws exception
         try {
            dataSource.transaction {
               mikrom.execute(
                  Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
                  listOf(2, "exception_name"),
               )
               throw RuntimeException("Simulated error")
            }
         } catch (e: RuntimeException) {
            // Expected exception
         }

         // Verify data was rolled back due to exception
         dataSource.transaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records ORDER BY id"))
            records shouldHaveSize 1
            records[0] shouldBe TestRecord(1, "initial_name")
         }
      }

      test("transaction can update existing data and commit") {
         // Insert initial data
         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
               listOf(1, "original_name"),
            )
         }

         // Update data and commit
         dataSource.transaction {
            mikrom.execute(
               Query("UPDATE test_records SET name = ? WHERE id = ?"),
               listOf("updated_name", 1),
            )
         }

         // Verify update was committed
         dataSource.transaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records"))
            records shouldHaveSize 1
            records[0] shouldBe TestRecord(1, "updated_name")
         }
      }

      test("transaction can update existing data and rollback") {

         // Insert initial data
         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO test_records (id, name) VALUES (?, ?)"),
               listOf(1, "original_name"),
            )
         }

         // Update data but rollback
         dataSource.transaction {
            mikrom.execute(
               Query("UPDATE test_records SET name = ? WHERE id = ?"),
               listOf("updated_name", 1),
            )
            Rollback
         }

         // Verify update was rolled back
         dataSource.transaction {
            val records = mikrom.queryFor<TestRecord>(Query("SELECT * FROM test_records"))
            records shouldHaveSize 1
            records[0] shouldBe TestRecord(1, "original_name")
         }
      }
   },
)
