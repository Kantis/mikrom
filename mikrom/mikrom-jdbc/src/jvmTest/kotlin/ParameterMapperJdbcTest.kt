package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.ParameterMapper
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.h2.prepareH2Database
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class Employee(val name: String, val age: Int)

class ParameterMapperJdbcTest : FunSpec(
   {
      test("execute with parameter mapper") {
         val mikrom =
            Mikrom {
               registerRowMapper { row ->
                  Employee(row.get("name"), row.get("age"))
               }
               registerParameterMapper<Employee> { employee ->
                  mapOf("name" to employee.name, "age" to employee.age)
               }
            }

         val dataSource = prepareH2Database(
            """
               CREATE TABLE employees (
                  name VARCHAR(255),
                  age INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO employees (name, age) VALUES (:name, :age)"),
               Employee("Alice", 30),
            )

            mikrom.queryFor<Employee>(
               Query("SELECT * FROM employees"),
            ) shouldBe listOf(Employee("Alice", 30))
         }
      }

      test("batch execute with parameter mapper") {
         val mikrom =
            Mikrom {
               registerParameterMapper<Employee>(
                  ParameterMapper { employee ->
                     mapOf("name" to employee.name, "age" to employee.age)
                  },
               )
            }

         val dataSource = prepareH2Database(
            """
               CREATE TABLE staff (
                  name VARCHAR(255),
                  age INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO staff (name, age) VALUES (:name, :age)"),
               Employee("Alice", 30),
               Employee("Bob", 25),
            )

            mikrom.queryFor<Int>(
               Query("SELECT COUNT(*) FROM staff"),
            ) shouldBe listOf(2)
         }
      }

      test("queryFor with resolveParameterMapper") {
         val mikrom =
            Mikrom {
               registerRowMapper { row ->
                  Employee(row.get("name"), row.get("age"))
               }
               registerParameterMapper<Employee> { employee ->
                  mapOf("name" to employee.name, "age" to employee.age)
               }
            }

         val dataSource = prepareH2Database(
            """
               CREATE TABLE workers (
                  name VARCHAR(255),
                  age INT
               )
            """.trimIndent(),
         )

         dataSource.transaction {
            mikrom.execute(
               Query("INSERT INTO workers (name, age) VALUES (:name, :age)"),
               Employee("Alice", 30),
               Employee("Bob", 25),
            )

            val filter = Employee("Alice", 30)
            val params = mikrom.resolveParameterMapper<Employee>().mapParameters(filter)

            mikrom.queryFor<Employee>(
               Query("SELECT * FROM workers WHERE name = :name"),
               params,
            ) shouldBe listOf(Employee("Alice", 30))
         }
      }
   },
)
