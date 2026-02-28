package io.github.kantis.mikrom

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
public class Row
   @PublishedApi
   internal constructor(private val columns: Map<String, Column>) {
      public data class Column(
         val value: Any?,
         val kotlinType: KClass<*>? = null,
         val sqlTypeName: String? = null,
      )

      public val columnNames: Set<String> get() = columns.keys

      public fun singleValue(): Any? {
         require(columns.size == 1) {
            "Expected exactly one column, but found ${columns.size}: $columnNames"
         }
         return columns.values.single().value
      }

      private fun resolveColumn(column: String): Column =
         columns[column]
            ?: throw NoSuchElementException(
               "Column '$column' not found. Available: $columnNames",
            )

      private fun typeMismatchMessage(
         column: String,
         col: Column,
         actual: String,
         expected: String,
      ): String {
         val typeInfo = col.sqlTypeName?.let { " ($it)" } ?: ""
         return "Column '$column'$typeInfo contains $actual, " +
            "cannot be read as $expected"
      }

      context(mikrom: Mikrom)
      public fun <T : Any> get(
         column: String,
         clazz: KClass<*>,
      ): T {
         val col = resolveColumn(column)

         val value = col.value
            ?: throw TypeMismatchException(
               "Column '$column' is null, " +
                  "but non-null ${clazz.simpleName} was expected",
            )

         val convertedValue = (mikrom.conversions.convert(value, clazz) as? T) ?: value

         return convertedValue as? T
            ?: throw TypeMismatchException(
               typeMismatchMessage(
                  column,
                  col,
                  value::class.simpleName ?: "Unknown",
                  clazz.simpleName ?: "Unknown",
               ),
            )
      }

      context(mikrom: Mikrom)
      public fun <T> getOrNull(
         column: String,
         clazz: KClass<*>,
      ): T? {
         val col = resolveColumn(column)
         val value = col.value ?: return null
         val convertedValue = (mikrom.conversions.convert(value, clazz) as? T) ?: value

         return convertedValue as? T
            ?: throw TypeMismatchException(
               typeMismatchMessage(
                  column,
                  col,
                  value::class.simpleName ?: "Unknown",
                  clazz.simpleName ?: "Unknown",
               ),
            )
      }

      override fun equals(other: Any?): Boolean {
         if (this === other) return true
         if (other !is Row) return false
         return columns == other.columns
      }

      override fun hashCode(): Int = columns.hashCode()

      override fun toString(): String = "Row(${columns.entries.joinToString { "${it.key}=${it.value.value}" }})"

      public companion object {
         public fun of(vararg pairs: Pair<String, Any?>): Row =
            Row(
               pairs.associate { (name, value) ->
                  name to Column(value)
               },
            )
      }
   }

context(mikrom: Mikrom)
public inline fun <reified T : Any> Row.get(column: String): T = get(column, T::class)

context(mikrom: Mikrom)
public inline fun <reified T> Row.getOrNull(column: String): T? = getOrNull(column, T::class)
