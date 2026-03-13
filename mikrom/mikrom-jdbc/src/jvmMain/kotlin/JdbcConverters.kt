package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.convert.TypeConverters
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.sql.DataSource

public fun jdbcConverters(dataSource: DataSource): TypeConverters {
   val driverName = dataSource.connection.use { it.metaData.driverName }
   return when {
      driverName.contains("Oracle", ignoreCase = true) -> oracleJdbcConverters()

      driverName.contains("MSSQL", ignoreCase = true) ||
         driverName.contains("Microsoft", ignoreCase = true) ||
         driverName.contains("SQL Server", ignoreCase = true) -> mssqlJdbcConverters()

      driverName.contains("SQLite", ignoreCase = true) -> sqliteJdbcConverters()

      else -> TypeConverters.EMPTY
   }
}

private fun oracleJdbcConverters(): TypeConverters =
   TypeConverters.Builder().apply {
      register<BigDecimal, Int> { it.intValueExact() }
      register<BigDecimal, Long> { it.longValueExact() }
      register<BigDecimal, Double> { it.toDouble() }
      register<BigDecimal, Boolean> { it.intValueExact() != 0 }
      register<Timestamp, LocalDate> { it.toLocalDateTime().toLocalDate() }
      register<Timestamp, LocalDateTime> { it.toLocalDateTime() }
   }.build()

private fun mssqlJdbcConverters(): TypeConverters =
   TypeConverters.Builder().apply {
      register<Int, Boolean> { it != 0 }
   }.build()

private fun sqliteJdbcConverters(): TypeConverters =
   TypeConverters.Builder().apply {
      register<Int, Boolean> { it != 0 }
      register<Int, BigDecimal> { BigDecimal(it) }
      register<Long, LocalDate> {
         Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
      }
      register<Long, LocalDateTime> {
         Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
      }
      register<String, LocalDate> { s ->
         s.toLongOrNull()?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
         } ?: LocalDate.parse(s)
      }
      register<String, LocalDateTime> { s ->
         s.toLongOrNull()?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
         } ?: LocalDateTime.parse(s)
      }
   }.build()
