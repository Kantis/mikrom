package io.github.kantis.mikrom

import io.github.kantis.mikrom.convert.TypeConverters
import kotlin.reflect.KClass

@MikromInternal
public class RowBuilder
   @PublishedApi
   internal constructor() {
      @PublishedApi
      internal val columns: MutableMap<String, Row.Column> = mutableMapOf()

      public fun column(
         name: String,
         value: Any?,
         kotlinType: KClass<*>? = null,
         sqlTypeName: String? = null,
      ) {
         columns[name] = Row.Column(value, kotlinType, sqlTypeName)
      }
   }

@MikromInternal
public inline fun buildRow(
   driverConverters: TypeConverters = TypeConverters.EMPTY,
   block: RowBuilder.() -> Unit,
): Row {
   val builder = RowBuilder()
   builder.block()
   return Row(builder.columns.toMap(), driverConverters)
}
