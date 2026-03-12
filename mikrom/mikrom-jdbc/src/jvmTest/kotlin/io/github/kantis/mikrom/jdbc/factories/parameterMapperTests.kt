package io.github.kantis.mikrom.jdbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.ParameterMapper
import io.github.kantis.mikrom.execute
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.JdbcTestDialect
import io.github.kantis.mikrom.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe

private data class Employee(val name: String, val age: Int)

fun parameterMapperTests(
   dialect: JdbcTestDialect,
   dataSourceProvider: () -> JdbcDataSource,
) = funSpec {
   beforeEach {
      dataSourceProvider().transaction {
         val mikrom = Mikrom {}
         mikrom.execute(dialect.truncateTable("people"))
      }
   }

   test("[${dialect.name}] execute with parameter mapper") {
      val mikrom = Mikrom {
         registerRowMapper { row ->
            Employee(row.get("name"), row.get("age"))
         }
         registerParameterMapper<Employee> { employee ->
            mapOf("name" to employee.name, "age" to employee.age)
         }
      }

      dataSourceProvider().transaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            Employee("Alice", 30),
         )

         mikrom.queryFor<Employee>(
            "SELECT * FROM people",
         ) shouldBe listOf(Employee("Alice", 30))
      }
   }

   test("[${dialect.name}] batch execute with parameter mapper") {
      val mikrom = Mikrom {
         registerParameterMapper<Employee>(
            ParameterMapper { employee ->
               mapOf("name" to employee.name, "age" to employee.age)
            },
         )
      }

      dataSourceProvider().transaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            Employee("Alice", 30),
            Employee("Bob", 25),
         )

         mikrom.queryFor<Long>(
            "SELECT COUNT(*) FROM people",
         ) shouldBe listOf(2L)
      }
   }

   test("[${dialect.name}] queryFor with ParameterMapper") {
      val mikrom = Mikrom {
         registerRowMapper { row ->
            Employee(row.get("name"), row.get("age"))
         }
         registerParameterMapper<Employee> { employee ->
            mapOf("name" to employee.name, "age" to employee.age)
         }
      }

      dataSourceProvider().transaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            Employee("Alice", 30),
            Employee("Bob", 25),
         )

         val filter = Employee("Alice", 30)
         mikrom.queryFor<Employee>(
            "SELECT * FROM people WHERE name = :name",
            filter,
         ) shouldBe listOf(Employee("Alice", 30))
      }
   }
}
