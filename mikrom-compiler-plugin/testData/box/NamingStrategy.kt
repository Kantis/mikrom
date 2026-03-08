// FIR_DUMP
// DUMP_IR

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.convert.TypeConverters
import io.github.kantis.mikrom.generator.NamingStrategy
import io.github.kantis.mikrom.generator.Column
import io.github.kantis.mikrom.generator.MikromResult
import kotlin.test.*

fun box(): String {
   // Test SNAKE_CASE (default)
   val snakeMikrom = Mikrom(mutableMapOf(), converters =TypeConverters.EMPTY)
   val invoice = Invoice.rowMapper().mapRow(
      Row.of(
         "invoice_id" to 42,
         "customer_name" to "Alice",
      ),
      snakeMikrom,
   )
   assertEquals(Invoice(42, "Alice"), invoice)

   // Test @Column overrides naming strategy
   val overridden = OverriddenEntity.rowMapper().mapRow(
      Row.of(
         "custom_col" to "hello",
         "other_field" to 99,
      ),
      snakeMikrom,
   )
   assertEquals(OverriddenEntity("hello", 99), overridden)

   // Test AS_IS preserves parameter names
   val asIsMikrom = Mikrom(mutableMapOf(), converters = TypeConverters.EMPTY, namingStrategy = NamingStrategy.AS_IS)
   val entity = AsIsEntity.rowMapper().mapRow(
      Row.of(
         "firstName" to "Bob",
      ),
      asIsMikrom,
   )
   assertEquals(AsIsEntity("Bob"), entity)

   return "OK"
}

@MikromResult
data class Invoice(
   val invoiceId: Int,
   val customerName: String,
)

@MikromResult
data class OverriddenEntity(
   @Column("custom_col") val name: String,
   val otherField: Int,
)

@MikromResult
data class AsIsEntity(
   val firstName: String,
)
