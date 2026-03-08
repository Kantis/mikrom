// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.convert.TypeConversions
import io.github.kantis.mikrom.generator.Column
import io.github.kantis.mikrom.generator.MikromResult
import kotlin.test.*

fun box(): String {
   val mikrom = Mikrom(mutableMapOf(), conversions = TypeConversions.EMPTY)
   val user = DbUser.rowMapper().mapRow(
      Row.of(
         "user_name" to "Alice",
         "email_address" to "alice@example.com",
         "age" to 30,
      ),
      mikrom,
   )

   assertEquals(user, DbUser("Alice", "alice@example.com", 30))
   return "OK"
}

@MikromResult
data class DbUser(
   @Column("user_name") val name: String,
   @Column("email_address") val email: String,
   val age: Int,
)
