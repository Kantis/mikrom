package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.convert.TypeConversions
import io.r2dbc.pool.ConnectionPool
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

public fun r2dbcConversions(pool: ConnectionPool): TypeConversions {
   val driverName = pool.metadata.name
   return when {
      driverName.contains("MySQL", ignoreCase = true) -> mysqlConversions()
      driverName.contains("Oracle", ignoreCase = true) -> oracleConversions()
      else -> TypeConversions.EMPTY
   }
}

private fun mysqlConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Byte, Boolean> { it.toInt() != 0 }
      register<Short, Boolean> { it.toInt() != 0 }
      register<Int, Boolean> { it != 0 }
      register<ZonedDateTime, LocalDateTime> { it.toLocalDateTime() }
   }.build()

private fun oracleConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<BigDecimal, Int> { it.intValueExact() }
      register<BigDecimal, Long> { it.longValueExact() }
      register<BigDecimal, Boolean> { it.intValueExact() != 0 }
      register<BigDecimal, Double> { it.toDouble() }
      register<LocalDateTime, LocalDate> { it.toLocalDate() }
   }.build()
