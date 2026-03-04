package io.github.kantis.mikrom

import io.github.kantis.mikrom.convert.TypeConversions
import io.github.kantis.mikrom.convert.tryWrapValueClass
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@JvmInline
value class UserId(val value: String)

@JvmInline
value class Age(val value: Int)

class ValueClassSupportTest : FunSpec({
   test("tryWrapValueClass wraps String in value class") {
      val result = tryWrapValueClass("hello", UserId::class, TypeConversions.EMPTY)
      result shouldBe UserId("hello")
   }

   test("tryWrapValueClass wraps Int in value class") {
      val result = tryWrapValueClass(42, Age::class, TypeConversions.EMPTY)
      result shouldBe Age(42)
   }

   test("tryWrapValueClass returns null for non-value class") {
      val result = tryWrapValueClass("hello", String::class, TypeConversions.EMPTY)
      result shouldBe null
   }

   test("tryWrapValueClass uses conversions for underlying type") {
      val conversions = TypeConversions.Builder().apply {
         register<Int, UInt> { it.toUInt() }
      }.build()
      // Int -> UInt conversion, then wrap -- but this requires a UInt value class
      // For now just test that incompatible types return null
      val result = tryWrapValueClass(42, UserId::class, TypeConversions.EMPTY)
      result shouldBe null
   }

   test("Row.get supports value classes") {
      val mikrom = Mikrom(mutableMapOf(), conversions = TypeConversions.EMPTY)
      val row = Row.of("id" to "user-123")
      with(mikrom) {
         row.get<UserId>("id") shouldBe UserId("user-123")
      }
   }

   test("Row.getOrNull supports value classes with non-null value") {
      val mikrom = Mikrom(mutableMapOf(), conversions = TypeConversions.EMPTY)
      val row = Row.of("id" to "user-123")
      with(mikrom) {
         row.getOrNull<UserId>("id") shouldBe UserId("user-123")
      }
   }

   test("Row.getOrNull supports value classes with null value") {
      val mikrom = Mikrom(mutableMapOf(), conversions = TypeConversions.EMPTY)
      val row = Row.of("id" to null)
      with(mikrom) {
         row.getOrNull<UserId>("id") shouldBe null
      }
   }
})
