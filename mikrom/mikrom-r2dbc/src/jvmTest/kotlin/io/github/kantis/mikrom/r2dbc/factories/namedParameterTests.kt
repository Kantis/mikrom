package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.getOrNull
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.R2dbcTestDialect
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe

private data class Person(val name: String, val age: Int?)

fun namedParameterTests(
   dialect: R2dbcTestDialect,
   dataSourceProvider: suspend () -> PooledR2dbcDataSource,
) = funSpec {
   val mikrom = Mikrom {
      registerRowMapper { row ->
         Person(row.get("name"), row.getOrNull("age"))
      }
   }

   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = dataSourceProvider()
   }

   beforeEach {
      dataSource.suspendingTransaction {
         mikrom.execute(dialect.truncateTable("people"))
      }
   }

   test("[${dialect.name}] insert and query with named parameters") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            mapOf("name" to "Alice", "age" to 30),
         )

         mikrom.queryFor<Person>(
            "SELECT * FROM people WHERE name = :name",
            mapOf("name" to "Alice"),
         ) shouldBe listOf(Person("Alice", 30))
      }
   }

   test("[${dialect.name}] batch with named parameters") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            mapOf("name" to "Alice", "age" to 30),
            mapOf("name" to "Bob", "age" to 25),
         )

         mikrom.queryFor<Long>("SELECT COUNT(*) FROM people") shouldBe listOf(2L)
      }
   }

   test("[${dialect.name}] null named parameters are bound with TypedNull") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            mapOf("name" to "Alice", "age" to TypedNull(Int::class)),
         )

         mikrom.queryFor<Person>(
            "SELECT * FROM people WHERE name = :name",
            mapOf("name" to "Alice"),
         ) shouldBe listOf(Person("Alice", null))
      }
   }

   test("[${dialect.name}] repeated named parameters") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            dialect.insertPerson(),
            listOf("Alice", 30),
            listOf("Bob", 25),
            listOf("Alice", 35),
         )

         mikrom.queryFor<Person>(
            "SELECT * FROM people WHERE name = :name OR name = :name ORDER BY age",
            mapOf("name" to "Alice"),
         ) shouldBe listOf(
            Person("Alice", 30),
            Person("Alice", 35),
         )
      }
   }
}
