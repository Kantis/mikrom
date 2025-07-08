package io.github.kantis.mikrom.r2dbc.stream

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.helpers.preparePostgresDatabase
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.executeStreaming
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.spec.style.FunSpec
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

class R2dbcDataTypesTest : FunSpec({
   val mikrom = Mikrom {
      registerRowMapper { row ->
         DataTypeRecord(
            id = row["id"] as Int,
            stringField = row["string_field"] as String?,
            intField = row["int_field"] as Int?,
            longField = row["long_field"] as Long?,
            booleanField = row["boolean_field"] as Boolean?,
            doubleField = row["double_field"] as Double?,
            decimalField = row["decimal_field"] as BigDecimal?,
            dateField = row["date_field"] as LocalDate?,
            timestampField = row["timestamp_field"] as LocalDateTime?,
            uuidField = row["uuid_field"] as UUID?,
         )
      }
   }

   lateinit var dataSource: PooledR2dbcDataSource

   beforeSpec {
      dataSource = preparePostgresDatabase(
         """
             CREATE TABLE data_types (
                 id SERIAL PRIMARY KEY,
                 string_field VARCHAR(255),
                 int_field INTEGER,
                 long_field BIGINT,
                 boolean_field BOOLEAN,
                 double_field DOUBLE PRECISION,
                 decimal_field DECIMAL(10,2),
                 date_field DATE,
                 timestamp_field TIMESTAMP,
                 uuid_field UUID
             )
         """.trimIndent(),
      )
   }

   beforeEach {
      dataSource.suspendingTransaction {
         println("Truncating data_types table")
         mikrom.execute(Query("TRUNCATE TABLE data_types"))
         println("Truncated data_types table")
      }
   }

   test("handle various PostgreSQL data types") {
      val testUuid = UUID.randomUUID()
      val testDate = LocalDate.of(2023, 12, 25)
      val testTimestamp = LocalDateTime.of(2023, 12, 25, 15, 30, 45)

      dataSource.suspendingTransaction {
         mikrom.executeStreaming(
            Query(
               """
                    INSERT INTO data_types (
                        string_field, int_field, long_field, boolean_field,
                        double_field, decimal_field, date_field, timestamp_field, uuid_field
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
               """.trimIndent(),
            ),
            flowOf(
               listOf(
                  "test string",
                  42,
                  1234567890L,
                  true,
                  3.14159,
                  BigDecimal("999.99"),
                  testDate,
                  testTimestamp,
                  testUuid,
               ),
            ),
         ).join()

         val records = mikrom.queryFor<DataTypeRecord>(Query("SELECT * FROM data_types")).toList()
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
         record.uuidField shouldBe testUuid
      }
   }

   test("handle null values") {
      dataSource.suspendingTransaction {
         mikrom.execute(
            Query(
               """
                    INSERT INTO data_types (
                        string_field, int_field, long_field, boolean_field,
                        double_field, decimal_field, date_field, timestamp_field, uuid_field
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
               """.trimIndent(),
            ),
            listOf(null, null, null, null, null, null, null, null, null),
         )

         val records = mikrom.queryFor<DataTypeRecord>(Query("SELECT * FROM data_types")).toList()
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
         record.uuidField shouldBe null
      }
   }
})
