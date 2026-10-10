package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.ParameterMapper
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.R2dbcTestDialect
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList

private data class Employee(val name: String, val age: Int)

fun parameterMapperTests(
   dialect: R2dbcTestDialect,
   dataSourceProvider: suspend () -> PooledR2dbcDataSource,
) = funSpec {
   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = dataSourceProvider()
   }

   beforeEach {
      dataSource.suspendingTransaction {
         Mikrom {}.execute(dialect.truncateTable("people"))
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

      dataSource.suspendingTransaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            Employee("Alice", 30),
         )

         mikrom.queryFor<Employee>("SELECT * FROM people").toList() shouldBe listOf(Employee("Alice", 30))
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

      dataSource.suspendingTransaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            Employee("Alice", 30),
            Employee("Bob", 25),
         )

         mikrom.queryFor<Long>("SELECT COUNT(*) FROM people").toList() shouldBe listOf(2L)
      }
   }

   test("[${dialect.name}] queryFor with parameter mapper") {
      val mikrom = Mikrom {
         registerRowMapper { row ->
            Employee(row.get("name"), row.get("age"))
         }
         registerParameterMapper<Employee> { employee ->
            mapOf("name" to employee.name, "age" to employee.age)
         }
      }

      dataSource.suspendingTransaction {
         mikrom.execute(
            "INSERT INTO people (name, age) VALUES (:name, :age)",
            Employee("Alice", 30),
            Employee("Bob", 25),
         )

         mikrom.queryFor<Employee>(
            "SELECT * FROM people WHERE name = :name",
            Employee("Alice", 30),
         ).toList() shouldBe listOf(Employee("Alice", 30))
      }
   }
}
