package io.github.kantis.mikrom

import java.sql.Timestamp
import java.time.Instant

public actual fun platformDefaultConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Timestamp, Instant> { it.toInstant() }
   }.build()
