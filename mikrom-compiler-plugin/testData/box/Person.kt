// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.generator.RowMapped
import kotlin.test.*

fun box(): String {
   val person = Person.RowMapper.mapRow(
      Row.of(
         "name" to "Brian",
         "nickname" to "bnorm",
         "age" to -1,
      ),
   )

   assertEquals(person, Person("Brian", "bnorm", -1))
   return "OK"
}

@RowMapped
data class Person(
   val name: String,
   val nickname: String? = name,
   val age: Int = 0,
)
