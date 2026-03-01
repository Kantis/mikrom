// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypeConversions
import io.github.kantis.mikrom.generator.RowMapped
import kotlin.test.*

@JvmInline
value class UserId(val value: String)

@JvmInline
value class Age(val value: Int)

@RowMapped
data class UserWithValueClass(
   val id: UserId,
   val age: Age,
)

fun box(): String {
   val mikrom = Mikrom(mutableMapOf(), TypeConversions.EMPTY)
   val user = UserWithValueClass.rowMapper().mapRow(
      Row.of(
         "id" to "user-123",
         "age" to 42,
      ),
      mikrom,
   )

   assertEquals(user, UserWithValueClass(UserId("user-123"), Age(42)))
   return "OK"
}
