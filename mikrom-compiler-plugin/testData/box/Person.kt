// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.convert.TypeConverters
import io.github.kantis.mikrom.generator.MikromResult
import kotlin.test.*

fun box(): String {
   val mikrom = Mikrom(mutableMapOf(), converters = TypeConverters.EMPTY)
   val person = Person.rowMapper().mapRow(
      Row.of(
         "name" to "Brian",
         "nickname" to "bnorm",
         "age" to -1,
      ),
      mikrom,
   )

   assertEquals(person, Person("Brian", "bnorm", -1))
   return "OK"
}

@MikromResult
data class Person(
   val name: String,
   val nickname: String? = name,
   val age: Int = 0,
)
