package io.github.kantis.mikrom

import io.github.kantis.mikrom.datasource.Transaction
import io.github.kantis.mikrom.execute
import org.intellij.lang.annotations.Language

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: List<Any?>,
) {
   transaction.executeInTransaction(query, params)
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: List<Any?>,
) {
   params.forEach { transaction.executeInTransaction(query, it) }
}

@Suppress("UNCHECKED_CAST")
context(transaction: Transaction)
public fun <T : Any> Mikrom.execute(
   @Language("SQL") query: Query,
   vararg params: T,
) {
   val parsed = parseNamedParameters(query)
   params.forEach {
      val resolved = resolveQueryParams(parsed, it)
      transaction.executeInTransaction(resolved.sql, resolved.params)
   }
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
) {
   transaction.executeInTransaction(query, emptyList<Any>())
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   params: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query)
   when (parsed) {
      is ParsedQuery.Named -> execute(parsed.sql, parsed.resolveParams(params))
      is ParsedQuery.Positional -> execute(parsed.sql, listOf(params))
   }
}

context(transaction: Transaction)
public fun Mikrom.execute(
   @Language("SQL") query: Query,
   vararg paramMaps: Map<String, Any?>,
) {
   val parsed = parseNamedParameters(query)
   when (parsed) {
      is ParsedQuery.Named -> paramMaps.forEach { execute(parsed.sql, parsed.resolveParams(it)) }
      is ParsedQuery.Positional -> paramMaps.forEach { execute(parsed.sql, listOf(it)) }
   }
}
