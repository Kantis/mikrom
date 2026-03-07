package io.github.kantis.mikrom.jdbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.JdbcTestDialect
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private data class TestRecord(val id: Int, val name: String)

fun transactionTests(
   dialect: JdbcTestDialect,
   dataSourceProvider: () -> JdbcDataSource,
) = funSpec {
   val mikrom = Mikrom {
      registerRowMapper { row ->
         TestRecord(
            row.get("id"),
            row.get("name"),
         )
      }
   }

   beforeEach {
      dataSourceProvider().transaction {
         mikrom.execute(dialect.truncateTable("test_records"))
      }
   }

   test("[${dialect.name}] transaction commits successfully") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("test record"),
         )
      }

      dataSourceProvider().transaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records")
         records shouldHaveSize 1
         records[0].name shouldBe "test record"
      }
   }

   test("[${dialect.name}] transaction rolls back on explicit rollback") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("initial record"),
         )
      }

      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("record to rollback"),
         )
         Rollback
      }

      dataSourceProvider().transaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records")
         records.shouldContainExactly(TestRecord(records[0].id, "initial record"))
      }
   }

   test("[${dialect.name}] transaction rolls back on exception") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("initial record"),
         )
      }

      try {
         dataSourceProvider().transaction {
            mikrom.execute(
               dialect.insertTestRecord(),
               listOf("exception record"),
            )
            throw RuntimeException("Simulated error")
         }
      } catch (_: RuntimeException) {
      }

      dataSourceProvider().transaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records")
         records shouldHaveSize 1
         records[0].name shouldBe "initial record"
      }
   }

   test("[${dialect.name}] transaction can update and commit") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("original_name"),
         )
      }

      dataSourceProvider().transaction {
         mikrom.execute(
            "UPDATE test_records SET name = ? WHERE name = ?",
            listOf("updated_name", "original_name"),
         )
      }

      dataSourceProvider().transaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records")
         records shouldHaveSize 1
         records[0].name shouldBe "updated_name"
      }
   }

   test("[${dialect.name}] transaction can update and rollback") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertTestRecord(),
            listOf("original_name"),
         )
      }

      dataSourceProvider().transaction {
         mikrom.execute(
            "UPDATE test_records SET name = ? WHERE name = ?",
            listOf("updated_name", "original_name"),
         )
         Rollback
      }

      dataSourceProvider().transaction {
         val records = mikrom.queryFor<TestRecord>("SELECT * FROM test_records")
         records shouldHaveSize 1
         records[0].name shouldBe "original_name"
      }
   }
}
