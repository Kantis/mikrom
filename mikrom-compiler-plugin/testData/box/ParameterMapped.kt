// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.generator.MikromParameter
import kotlin.test.*

fun box(): String {
   val mapper = Employee.parameterMapper()
   val params = mapper.mapParameters(Employee("Alice", 30))
   assertEquals("Alice", params["name"])
   assertEquals(30, params["age"])
   assertEquals(2, params.size)
   return "OK"
}

@MikromParameter
data class Employee(
   val name: String,
   val age: Int,
)
