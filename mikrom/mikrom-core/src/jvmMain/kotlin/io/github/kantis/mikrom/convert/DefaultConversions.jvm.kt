package io.github.kantis.mikrom.convert

import java.nio.ByteBuffer
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

public actual fun platformDefaultConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Timestamp, Instant> { it.toInstant() }
      register<Timestamp, LocalDateTime> { it.toLocalDateTime() }
      register<Date, LocalDate> { it.toLocalDate() }
      register<ByteArray, UUID> {
         val buf = ByteBuffer.wrap(it)
         UUID(buf.long, buf.long)
      }
   }.build()
