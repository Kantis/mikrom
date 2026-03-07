package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.convert.TypeConversions
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

public fun jdbcConversions(dataSource: DataSource): TypeConversions {
   val driverName = dataSource.connection.use { it.metaData.driverName }
   return when {
      driverName.contains("Oracle", ignoreCase = true) -> oracleJdbcConversions()

      driverName.contains("MSSQL", ignoreCase = true) ||
         driverName.contains("Microsoft", ignoreCase = true) ||
         driverName.contains("SQL Server", ignoreCase = true) -> mssqlJdbcConversions()

      else -> TypeConversions.EMPTY
   }
}

private fun oracleJdbcConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<BigDecimal, Int> { it.intValueExact() }
      register<BigDecimal, Long> { it.longValueExact() }
      register<BigDecimal, Double> { it.toDouble() }
      register<BigDecimal, Boolean> { it.intValueExact() != 0 }
      register<Timestamp, LocalDate> { it.toLocalDateTime().toLocalDate() }
      register<Timestamp, LocalDateTime> { it.toLocalDateTime() }
   }.build()

private fun mssqlJdbcConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Int, Boolean> { it != 0 }
   }.build()
