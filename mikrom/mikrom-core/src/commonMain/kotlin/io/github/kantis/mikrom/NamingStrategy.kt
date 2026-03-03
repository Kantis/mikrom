package io.github.kantis.mikrom

public fun interface NamingStrategy {
   public fun toColumnName(parameterName: String): String

   public companion object {
      public val AS_IS: NamingStrategy = NamingStrategy { it }

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

      public val UPPER_SNAKE_CASE: NamingStrategy = NamingStrategy { name ->
         SNAKE_CASE.toColumnName(name).uppercase()
      }
   }
}
