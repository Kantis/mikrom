package io.github.kantis.mikrom.jdbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.getOrNull
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.JdbcTestDialect
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe

private data class Person(val name: String, val age: Int?)

fun namedParamTests(
   dialect: JdbcTestDialect,
   dataSourceProvider: () -> JdbcDataSource,
) = funSpec {
   val mikrom = Mikrom {
      registerRowMapper { row ->
         Person(row.get("name"), row.getOrNull("age"))
      }
   }

   beforeEach {
      dataSourceProvider().transaction {
         mikrom.execute(dialect.truncateTable("people"))
      }
   }

   test("[${dialect.name}] insert and query with named parameters") {
      dataSourceProvider().transaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            mapOf("name" to "Alice", "age" to 30),
         )

         mikrom.queryFor<Person, _>(
            "SELECT * FROM people WHERE name = :name",
            mapOf("name" to "Alice"),
         ) shouldBe listOf(Person("Alice", 30))
      }
   }

   test("[${dialect.name}] batch with named parameters") {
      dataSourceProvider().transaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            mapOf("name" to "Alice", "age" to 30),
            mapOf("name" to "Bob", "age" to 25),
         )

         mikrom.queryFor<Long>("SELECT COUNT(*) FROM people") shouldBe listOf(2L)
      }
   }

   test("[${dialect.name}] null named parameters") {
      dataSourceProvider().transaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            mapOf("name" to "Alice", "age" to null),
         )

         mikrom.queryFor<Person, _>(
            "SELECT * FROM people WHERE name = :name",
            mapOf("name" to "Alice"),
         ) shouldBe listOf(Person("Alice", null))
      }
   }

   test("[${dialect.name}] repeated named parameters") {
      dataSourceProvider().transaction {
         mikrom.execute(
            dialect.insertPerson(),
            listOf("Alice", 30),
            listOf("Bob", 25),
            listOf("Alice", 35),
         )

         mikrom.queryFor<Person, _>(
            "SELECT * FROM people WHERE name = :name OR name = :name ORDER BY age",
            mapOf("name" to "Alice"),
         ) shouldBe listOf(
            Person("Alice", 30),
            Person("Alice", 35),
         )
      }
   }
}
