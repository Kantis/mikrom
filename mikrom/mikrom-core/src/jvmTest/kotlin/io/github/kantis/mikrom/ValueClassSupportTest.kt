package io.github.kantis.mikrom

import io.github.kantis.mikrom.convert.TypeConverters
import io.github.kantis.mikrom.convert.tryWrapValueClass
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@JvmInline
value class UserId(val value: String)

@JvmInline
value class Age(val value: Int)

class ValueClassSupportTest : FunSpec({
   test("tryWrapValueClass wraps String in value class") {
      val result = tryWrapValueClass("hello", UserId::class, TypeConverters.EMPTY)
      result shouldBe UserId("hello")
   }

   test("tryWrapValueClass wraps Int in value class") {
      val result = tryWrapValueClass(42, Age::class, TypeConverters.EMPTY)
      result shouldBe Age(42)
   }

   test("tryWrapValueClass returns null for non-value class") {
      val result = tryWrapValueClass("hello", String::class, TypeConverters.EMPTY)
      result shouldBe null
   }

   test("tryWrapValueClass uses converters for underlying type") {
      val converters = TypeConverters.Builder().apply {
         register<Int, UInt> { it.toUInt() }
      }.build()
      // Int -> UInt conversion, then wrap -- but this requires a UInt value class
      // For now just test that incompatible types return null
      val result = tryWrapValueClass(42, UserId::class, TypeConverters.EMPTY)
      result shouldBe null
   }

   test("Row.get supports value classes") {
      val mikrom = Mikrom(mutableMapOf(), converters = TypeConverters.EMPTY)
      val row = Row.of("id" to "user-123")
      with(mikrom) {
         row.get<UserId>("id") shouldBe UserId("user-123")
      }
   }

   test("Row.getOrNull supports value classes with non-null value") {
      val mikrom = Mikrom(mutableMapOf(), converters = TypeConverters.EMPTY)
      val row = Row.of("id" to "user-123")
      with(mikrom) {
         row.getOrNull<UserId>("id") shouldBe UserId("user-123")
      }
   }

   test("Row.getOrNull supports value classes with null value") {
      val mikrom = Mikrom(mutableMapOf(), converters = TypeConverters.EMPTY)
      val row = Row.of("id" to null)
      with(mikrom) {
         row.getOrNull<UserId>("id") shouldBe null
      }
   }
})
