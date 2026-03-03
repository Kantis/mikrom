package io.github.kantis.mikrom

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

public actual fun platformDefaultConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Timestamp, Instant> { it.toInstant() }
      register<Timestamp, LocalDateTime> { it.toLocalDateTime() }
   }.build()
