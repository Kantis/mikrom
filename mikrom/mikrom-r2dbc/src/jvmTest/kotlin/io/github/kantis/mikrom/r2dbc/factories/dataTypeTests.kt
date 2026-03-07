package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.getOrNull
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.R2dbcTestDialect
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.executeStreaming
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private data class DataTypeRecord(
   val id: Int,
   val stringField: String?,
   val intField: Int?,
   val longField: Long?,
   val booleanField: Boolean?,
   val doubleField: Double?,
   val decimalField: BigDecimal?,
   val dateField: LocalDate?,
   val timestampField: LocalDateTime?,
   val uuidField: UUID?,
)

fun dataTypeTests(
   dialect: R2dbcTestDialect,
   dataSourceProvider: suspend () -> PooledR2dbcDataSource,
) = funSpec {
   val mikrom = Mikrom {
      registerRowMapper { row ->
         DataTypeRecord(
            id = row.get("id"),
            stringField = row.getOrNull("string_field"),
            intField = row.getOrNull("int_field"),
            longField = row.getOrNull("long_field"),
            booleanField = row.getOrNull("boolean_field"),
            doubleField = row.getOrNull("double_field"),
            decimalField = row.getOrNull("decimal_field"),
            dateField = row.getOrNull("date_field"),
            timestampField = row.getOrNull("timestamp_field"),
            uuidField = if (dialect.supportsUuid) row.getOrNull("uuid_field") else null,
         )
      }
   }

   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = dataSourceProvider()
   }

   beforeEach {
      dataSource.suspendingTransaction {
         mikrom.execute(dialect.truncateTable("data_types"))
      }
   }

   test("[${dialect.name}] handle core data types") {
      val testDate = LocalDate.of(2023, 12, 25)
      val testTimestamp = LocalDateTime.of(2023, 12, 25, 15, 30, 45)

      val params = buildList<Any> {
         add("test string")
         add(42)
         add(1234567890L)
         add(true)
         add(3.14159)
         add(BigDecimal("999.99"))
         add(testDate)
         add(testTimestamp)
         if (dialect.supportsUuid) add(UUID.randomUUID())
      }

      dataSource.suspendingTransaction {
         mikrom.executeStreaming(
            dialect.insertDataTypes(),
            flowOf(params),
         ).join()

         val records = mikrom.queryFor<DataTypeRecord>("SELECT * FROM data_types").toList()
         records.size shouldBe 1

         val record = records[0]
         record.stringField shouldBe "test string"
         record.intField shouldBe 42
         record.longField shouldBe 1234567890L
         record.booleanField shouldBe true
         record.doubleField shouldBe 3.14159
         record.decimalField shouldBe BigDecimal("999.99")
         record.dateField shouldBe testDate
         record.timestampField shouldBe testTimestamp
      }
   }

   test("[${dialect.name}] handle null values") {
      val nullCount = if (dialect.supportsUuid) 9 else 8
      val nullParams = List(nullCount) { null }

      dataSource.suspendingTransaction {
         mikrom.execute(
            dialect.insertDataTypes(),
            nullParams,
         )

         val records = mikrom.queryFor<DataTypeRecord>("SELECT * FROM data_types").toList()
         records.size shouldBe 1

         val record = records[0]
         record.stringField shouldBe null
         record.intField shouldBe null
         record.longField shouldBe null
         record.booleanField shouldBe null
         record.doubleField shouldBe null
         record.decimalField shouldBe null
         record.dateField shouldBe null
         record.timestampField shouldBe null
      }
   }

   if (dialect.supportsUuid) {
      test("[${dialect.name}] handle UUID values") {
         val testUuid = UUID.randomUUID()

         dataSource.suspendingTransaction {
            mikrom.executeStreaming(
               dialect.insertDataTypes(),
               flowOf(
                  listOf(
                     "uuid test",
                     1,
                     1L,
                     true,
                     1.0,
                     BigDecimal("1.00"),
                     LocalDate.of(2023, 1, 1),
                     LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                     testUuid,
                  ),
               ),
            ).join()

            val records = mikrom.queryFor<DataTypeRecord>("SELECT * FROM data_types").toList()
            records.size shouldBe 1
            records[0].uuidField shouldBe testUuid
         }
      }
   }
}
