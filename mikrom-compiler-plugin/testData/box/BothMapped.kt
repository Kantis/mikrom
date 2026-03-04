// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.convert.TypeConversions
import io.github.kantis.mikrom.generator.ParameterMapped
import io.github.kantis.mikrom.generator.RowMapped
import kotlin.test.*

fun box(): String {
   val mikrom = Mikrom(mutableMapOf(), conversions = TypeConversions.EMPTY)

   // Test RowMapper
   val person = Person.rowMapper().mapRow(
      Row.of("name" to "Brian", "age" to 42),
      mikrom,
   )
   assertEquals(Person("Brian", 42), person)

   // Test ParameterMapper
   val params = Person.parameterMapper().mapParameters(Person("Brian", 42))
   assertEquals("Brian", params["name"])
   assertEquals(42, params["age"])
   assertEquals(2, params.size)

   return "OK"
}

@RowMapped
@ParameterMapped
data class Person(
   val name: String,
   val age: Int,
)
