package io.github.kantis.mikrom

public fun commonDefaultConversions(): TypeConversions =
   TypeConversions.Builder().apply {
      register<Int, UInt> { it.toUInt() }
      register<Int, UByte> { it.toUByte() }
      register<Int, UShort> { it.toUShort() }
      register<Int, Byte> { it.toByte() }
      register<Int, Short> { it.toShort() }
   }.build()

public expect fun platformDefaultConversions(): TypeConversions

public fun defaultConversions(): TypeConversions = commonDefaultConversions() + platformDefaultConversions()
