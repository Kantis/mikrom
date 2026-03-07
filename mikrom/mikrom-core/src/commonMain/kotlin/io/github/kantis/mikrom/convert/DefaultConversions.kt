package io.github.kantis.mikrom.convert

public fun commonDefaultConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Int, UInt> { it.toUInt() }
      register<Int, UByte> { it.toUByte() }
      register<Int, UShort> { it.toUShort() }
      register<Int, Byte> { it.toByte() }
      register<Int, Short> { it.toShort() }
      register<Long, Int> { it.toInt() }
      register<Int, Long> { it.toLong() }
      register<Double, Int> { it.toInt() }
      register<Double, Long> { it.toLong() }
   }.build()

public expect fun platformDefaultConversions(): TypeConversions

public fun defaultConversions(): TypeConversions = commonDefaultConversions() + platformDefaultConversions()
