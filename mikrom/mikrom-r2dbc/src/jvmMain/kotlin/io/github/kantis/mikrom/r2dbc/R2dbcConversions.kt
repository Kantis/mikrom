package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.convert.TypeConversions
import io.r2dbc.pool.ConnectionPool
import java.time.LocalDateTime
import java.time.ZonedDateTime

public fun r2dbcConversions(pool: ConnectionPool): TypeConversions {
   val driverName = pool.metadata.name
   return when {
      driverName.contains("MySQL", ignoreCase = true) -> mysqlConversions()
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
