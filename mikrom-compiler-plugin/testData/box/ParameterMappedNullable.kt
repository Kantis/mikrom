// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.generator.MikromParameter
import kotlin.test.*

@MikromParameter
data class NullableFields(
   val name: String,
   val nickname: String?,
   val age: Int?,
)

fun box(): String {
   val mapper = NullableFields.parameterMapper()

   // Non-null values should pass through normally
   val params1 = mapper.mapParameters(NullableFields("Alice", "Ali", 30))
   assertEquals("Alice", params1["name"])
   assertEquals("Ali", params1["nickname"])
   assertEquals(30, params1["age"])

   // Null values should be wrapped in TypedNull
   val params2 = mapper.mapParameters(NullableFields("Bob", null, null))
   assertEquals("Bob", params2["name"])

   val nicknameValue = params2["nickname"]
   assertTrue(nicknameValue is TypedNull, "Expected TypedNull for null nickname, got: $nicknameValue")
   assertEquals(String::class, nicknameValue.type)

   val ageValue = params2["age"]
   assertTrue(ageValue is TypedNull, "Expected TypedNull for null age, got: $ageValue")
   assertEquals(Int::class, ageValue.type)

   return "OK"
}
