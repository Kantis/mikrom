package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.convert.TypeConverters
import io.r2dbc.pool.ConnectionPool
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

public fun r2dbcConverters(pool: ConnectionPool): TypeConverters {
   val driverName = pool.metadata.name
   return when {
      driverName.contains("MySQL", ignoreCase = true) -> mysqlConverters()
      driverName.contains("Oracle", ignoreCase = true) -> oracleConverters()
      else -> TypeConverters.EMPTY
   }
}

private fun mysqlConverters(): TypeConverters =
   TypeConverters.Builder().apply {
      register<Byte, Boolean> { it.toInt() != 0 }
      register<Short, Boolean> { it.toInt() != 0 }
      register<Int, Boolean> { it != 0 }
      register<ZonedDateTime, LocalDateTime> { it.toLocalDateTime() }
   }.build()

private fun oracleConverters(): TypeConverters =
   TypeConverters.Builder().apply {
      register<BigDecimal, Int> { it.intValueExact() }
      register<BigDecimal, Long> { it.longValueExact() }
      register<BigDecimal, Boolean> { it.intValueExact() != 0 }
      register<BigDecimal, Double> { it.toDouble() }
      register<LocalDateTime, LocalDate> { it.toLocalDate() }
   }.build()
