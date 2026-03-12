package io.github.kantis.mikrom.convert

public fun commonDefaultConverters(): TypeConverters =
   TypeConverters.Builder().apply {
      register<Int, UInt> { it.toUInt() }
      register<Int, UByte> { it.toUByte() }
      register<Int, UShort> { it.toUShort() }
      register<Int, Byte> { it.toByte() }
      register<Int, Short> { it.toShort() }
      register<Long, Int> { it.toInt() }
      register<Int, Long> { it.toLong() }
   }.build()

public expect fun platformDefaultConverters(): TypeConverters

public fun defaultConverters(): TypeConverters = commonDefaultConverters() + platformDefaultConverters()
