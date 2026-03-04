package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.h2.prepareH2Database
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class Person(val name: String, val age: Int)

class NamedParamsJdbcTest : FunSpec(
   {
      test("insert and query with named parameters") {
         val mikrom =
            Mikrom {
               registerRowMapper { row ->
                  Person(row.get("name"), row.get("age"))
               }
            }

         val dataSource = prepareH2Database(
            """
               CREATE TABLE people (
                  name VARCHAR(255),
                  age INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO people (name, age) VALUES (:name, :age)"),
               mapOf("name" to "Alice", "age" to 30),
            )

            mikrom.queryFor<Person>(
               Query("SELECT * FROM people WHERE name = :name"),
               mapOf("name" to "Alice"),
            ) shouldBe listOf(Person("Alice", 30))
         }
      }

      test("batch execute with named parameters") {
         val mikrom = Mikrom {}

         val dataSource = prepareH2Database(
            """
               CREATE TABLE items (
                  label VARCHAR(255),
                  quantity INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO items (label, quantity) VALUES (:label, :quantity)"),
               mapOf("label" to "Apples", "quantity" to 5),
               mapOf("label" to "Bananas", "quantity" to 12),
            )

            mikrom.queryFor<Int>(
               Query("SELECT COUNT(*) FROM items"),
            ) shouldBe listOf(2)
         }
      }

      test("repeated named parameter in WHERE clause") {
         val mikrom =
            Mikrom {
               registerRowMapper { row ->
                  Person(row.get("name"), row.get("age"))
               }
            }

         val dataSource = prepareH2Database(
            """
               CREATE TABLE contacts (
                  name VARCHAR(255),
                  age INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO contacts (name, age) VALUES (?, ?)"),
               listOf("Alice", 30),
               listOf("Bob", 25),
               listOf("Alice", 35),
            )

            mikrom.queryFor<Person>(
               Query("SELECT * FROM contacts WHERE name = :name OR name = :name ORDER BY age"),
               mapOf("name" to "Alice"),
            ) shouldBe listOf(
               Person("Alice", 30),
               Person("Alice", 35),
            )
         }
      }
   },
)
