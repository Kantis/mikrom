// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.generator.ParameterMapped
import kotlin.test.*

@JvmInline
value class UserId(val value: String)

@JvmInline
value class Age(val value: Int)

@ParameterMapped
data class UserParams(val id: UserId, val age: Age)

fun box(): String {
   val mapper = UserParams.parameterMapper()
   val params = mapper.mapParameters(UserParams(UserId("user-123"), Age(42)))
   assertEquals("user-123", params["id"])
   assertEquals(42, params["age"])
   return "OK"
}
