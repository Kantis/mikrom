package io.github.kantis.mikrom.generator

import kotlin.text.iterator

/**
 * Transforms Kotlin parameter names into database column names.
 *
 * Configure on a [io.github.kantis.mikrom.Mikrom] instance to automatically map constructor parameter
 * names to column names. An explicit [@Column][Column]
 * annotation always takes precedence over the naming strategy.
 */
public fun interface NamingStrategy {
   public fun toColumnName(parameterName: String): String

   public companion object {
      /**
       * Identity strategy — returns the parameter name unchanged.
       *
       * ```
       * "invoiceId"    -> "invoiceId"
       * "customerName" -> "customerName"
       * ```
       */
      public val AS_IS: NamingStrategy = NamingStrategy { it }

      /**
       * Converts camelCase parameter names to lower snake_case column names.
       * This is the default strategy.
       *
       * ```
       * "invoiceId"    -> "invoice_id"
       * "customerName" -> "customer_name"
       * "age"          -> "age"
       * ```
       */
      public val SNAKE_CASE: NamingStrategy = NamingStrategy { name ->
         buildString {
            for (char in name) {
               if (char.isUpperCase()) {
                  if (isNotEmpty()) append('_')
                  append(char.lowercase())
               } else {
                  append(char)
               }
            }
         }
      }

      /**
       * Converts camelCase parameter names to UPPER_SNAKE_CASE column names.
       *
       * ```
       * "invoiceId"    -> "INVOICE_ID"
       * "customerName" -> "CUSTOMER_NAME"
       * "age"          -> "AGE"
       * ```
       */
      public val UPPER_SNAKE_CASE: NamingStrategy = NamingStrategy { name ->
         SNAKE_CASE.toColumnName(name).uppercase()
      }
   }
}
