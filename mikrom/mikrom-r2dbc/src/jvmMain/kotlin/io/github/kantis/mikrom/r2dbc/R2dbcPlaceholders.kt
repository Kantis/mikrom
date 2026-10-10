package io.github.kantis.mikrom.r2dbc

import io.r2dbc.pool.ConnectionPool

/**
 * Resolves the positional placeholder style of the driver behind [pool], used when Mikrom rewrites `:named`
 * parameters. PostgreSQL only accepts `$1` and SQL Server `@P1`; the other supported drivers accept `?`.
 */
public fun r2dbcPlaceholder(pool: ConnectionPool): (Int) -> String {
   val driverName = pool.metadata.name
   return when {
      driverName.contains("PostgreSQL", ignoreCase = true) -> { index -> "$$index" }

      driverName.contains("SQL Server", ignoreCase = true) ||
         driverName.contains("Microsoft", ignoreCase = true) -> { index -> "@P$index" }

      else -> { _ -> "?" }
   }
}
