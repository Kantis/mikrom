package io.github.kantis.mikrom

/**
 * Result of parsing a SQL query with named parameters.
 * [sql] contains the rewritten query with `?` placeholders.
 * [parameterNames] contains the ordered list of parameter names as they appear in the query.
 */
@MikromInternal
public data class ParsedQuery(
   val sql: String,
   val parameterNames: List<String>,
)

/**
 * Parses a SQL string containing `:paramName` style named parameters and
 * returns a [ParsedQuery] with positional `?` placeholders.
 *
 * Handles:
 * - `::identifier` PostgreSQL-style casts (not treated as parameters)
 * - Single-quoted and double-quoted string literals (parameters inside are not replaced)
 * - `--` line comments and `/ * ... * /` block comments (parameters inside are not replaced)
 * - Repeated parameter names (same name appears multiple times, resolved from the same map entry)
 */
@MikromInternal
public fun parseNamedParameters(sql: String): ParsedQuery {
   val result = StringBuilder(sql.length)
   val parameterNames = mutableListOf<String>()
   var i = 0
   var state = ParserState.NORMAL

   while (i < sql.length) {
      val c = sql[i]
      when (state) {
         ParserState.NORMAL -> {
            when {
               c == '\'' -> {
                  state = ParserState.IN_SINGLE_QUOTE
                  result.append(c)
                  i++
               }

               c == '"' -> {
                  state = ParserState.IN_DOUBLE_QUOTE
                  result.append(c)
                  i++
               }

               c == '-' && i + 1 < sql.length && sql[i + 1] == '-' -> {
                  state = ParserState.IN_LINE_COMMENT
                  result.append("--")
                  i += 2
               }

               c == '/' && i + 1 < sql.length && sql[i + 1] == '*' -> {
                  state = ParserState.IN_BLOCK_COMMENT
                  result.append("/*")
                  i += 2
               }

               c == ':' -> {
                  // Check for :: (PostgreSQL cast) — skip
                  if (i + 1 < sql.length && sql[i + 1] == ':') {
                     result.append("::")
                     i += 2
                  } else if (i + 1 < sql.length && sql[i + 1].isParamStart()) {
                     // Named parameter
                     i++ // skip the ':'
                     val nameStart = i
                     while (i < sql.length && sql[i].isParamPart()) {
                        i++
                     }
                     val name = sql.substring(nameStart, i)
                     parameterNames.add(name)
                     result.append('?')
                  } else {
                     result.append(c)
                     i++
                  }
               }

               else -> {
                  result.append(c)
                  i++
               }
            }
         }

         ParserState.IN_SINGLE_QUOTE -> {
            result.append(c)
            if (c == '\'') {
               // Handle escaped quotes ('')
               if (i + 1 < sql.length && sql[i + 1] == '\'') {
                  result.append('\'')
                  i += 2
               } else {
                  state = ParserState.NORMAL
                  i++
               }
            } else {
               i++
            }
         }

         ParserState.IN_DOUBLE_QUOTE -> {
            result.append(c)
            if (c == '"') {
               state = ParserState.NORMAL
            }
            i++
         }

         ParserState.IN_LINE_COMMENT -> {
            result.append(c)
            if (c == '\n') {
               state = ParserState.NORMAL
            }
            i++
         }

         ParserState.IN_BLOCK_COMMENT -> {
            result.append(c)
            if (c == '*' && i + 1 < sql.length && sql[i + 1] == '/') {
               result.append('/')
               state = ParserState.NORMAL
               i += 2
            } else {
               i++
            }
         }
      }
   }

   return ParsedQuery(result.toString(), parameterNames)
}

/**
 * Resolves named parameters from a [ParsedQuery] using the provided [params] map.
 * Returns an ordered list of parameter values matching the positional `?` placeholders.
 *
 * @throws IllegalArgumentException if any named parameter from the query is missing in the map.
 */
@MikromInternal
public fun ParsedQuery.resolveParams(params: Map<String, Any?>): List<Any?> {
   val missing = parameterNames.filter { it !in params }
   if (missing.isNotEmpty()) {
      val uniqueMissing = missing.distinct()
      error(
         "Missing named parameter${if (uniqueMissing.size > 1) "s" else ""}: " +
            "${uniqueMissing.joinToString(", ") { ":$it" }}. " +
            "Available parameters: ${params.keys.joinToString(", ") { ":$it" }}",
      )
   }
   return parameterNames.map { params[it] }
}

private enum class ParserState {
   NORMAL,
   IN_SINGLE_QUOTE,
   IN_DOUBLE_QUOTE,
   IN_LINE_COMMENT,
   IN_BLOCK_COMMENT,
}

private fun Char.isParamStart(): Boolean = isLetter() || this == '_'

private fun Char.isParamPart(): Boolean = isLetterOrDigit() || this == '_'
