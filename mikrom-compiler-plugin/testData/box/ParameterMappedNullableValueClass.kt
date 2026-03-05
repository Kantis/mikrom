// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.generator.ParameterMapped
import kotlin.test.*

@JvmInline
value class Email(val value: String)

@ParameterMapped
data class UserWithEmail(
   val name: String,
   val email: Email?,
)

fun box(): String {
   val mapper = UserWithEmail.parameterMapper()

   // Non-null value class should be unwrapped
   val params1 = mapper.mapParameters(UserWithEmail("Alice", Email("alice@example.com")))
   assertEquals("Alice", params1["name"])
   assertEquals("alice@example.com", params1["email"])

   // Null value class should be TypedNull with the underlying type
   val params2 = mapper.mapParameters(UserWithEmail("Bob", null))
   assertEquals("Bob", params2["name"])

   val emailValue = params2["email"]
   assertTrue(emailValue is TypedNull, "Expected TypedNull for null email, got: $emailValue")
   assertEquals(String::class, emailValue.type)

   return "OK"
}
