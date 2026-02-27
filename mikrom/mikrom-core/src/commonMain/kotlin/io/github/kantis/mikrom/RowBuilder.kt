package io.github.kantis.mikrom

import kotlin.reflect.KClass

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

public inline fun buildRow(block: RowBuilder.() -> Unit): Row {
   val builder = RowBuilder()
   builder.block()
   return Row(builder.columns.toMap())
}
