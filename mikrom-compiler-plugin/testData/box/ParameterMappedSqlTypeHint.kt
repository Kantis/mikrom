// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.AnsiString
import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.generator.MikromParameter
import io.github.kantis.mikrom.generator.SqlType
import io.github.kantis.mikrom.generator.SqlTypeHint
import kotlin.test.*

@MikromParameter
data class VarcharUser(
   @SqlTypeHint(SqlType.VARCHAR)
   val name: String,
   val age: Int,
)

@MikromParameter
data class NullableVarcharUser(
   @SqlTypeHint(SqlType.VARCHAR)
   val name: String?,
   val age: Int?,
)

fun box(): String {
   // Non-nullable: @SqlTypeHint(SqlType.VARCHAR) should wrap String in AnsiString
   val mapper = VarcharUser.parameterMapper()
   val params = mapper.mapParameters(VarcharUser("Alice", 30))

   val nameValue = params["name"]
   assertTrue(nameValue is AnsiString, "Expected AnsiString for name, got: $nameValue (${nameValue?.let { it::class }})")
   assertEquals("Alice", nameValue.value)
   assertEquals(30, params["age"])

   // Nullable: non-null value should be wrapped in AnsiString
   val nullableMapper = NullableVarcharUser.parameterMapper()
   val params2 = nullableMapper.mapParameters(NullableVarcharUser("Bob", 25))

   val nameValue2 = params2["name"]
   assertTrue(nameValue2 is AnsiString, "Expected AnsiString for non-null nullable name, got: $nameValue2 (${nameValue2?.let { it::class }})")
   assertEquals("Bob", nameValue2.value)
   assertEquals(25, params2["age"])

   // Nullable: null value should still be TypedNull
   val params3 = nullableMapper.mapParameters(NullableVarcharUser(null, null))
   val nameValue3 = params3["name"]
   assertTrue(nameValue3 is TypedNull, "Expected TypedNull for null name, got: $nameValue3")
   assertEquals(String::class, nameValue3.type)

   val ageValue3 = params3["age"]
   assertTrue(ageValue3 is TypedNull, "Expected TypedNull for null age, got: $ageValue3")
   assertEquals(Int::class, ageValue3.type)

   return "OK"
}
