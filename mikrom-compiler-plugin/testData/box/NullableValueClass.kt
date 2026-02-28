// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypeConversions
import io.github.kantis.mikrom.generator.RowMapped
import kotlin.test.*

@JvmInline
value class Email(val value: String)

@RowMapped
data class Contact(
   val name: String,
   val email: Email?,
)

fun box(): String {
   val mikrom = Mikrom(mutableMapOf(), TypeConversions.EMPTY)

   // Test with non-null value
   val contact1 = Contact.rowMapper().mapRow(
      Row.of(
         "name" to "Alice",
         "email" to "alice@example.com",
      ),
      mikrom,
   )
   assertEquals(contact1, Contact("Alice", Email("alice@example.com")))

   // Test with null value
   val contact2 = Contact.rowMapper().mapRow(
      Row.of(
         "name" to "Bob",
         "email" to null,
      ),
      mikrom,
   )
   assertEquals(contact2, Contact("Bob", null))

   return "OK"
}
