package io.github.kantis.mikrom.jdbc.sqlite

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.intellij.lang.annotations.Language
import java.sql.DriverManager

fun Spec.prepareSqliteDatabase(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val dbName = this::class.simpleName?.lowercase() ?: error("Unknown spec")
   val connectionString = "jdbc:sqlite:file:$dbName?mode=memory&cache=shared"
   DriverManager.registerDriver(org.sqlite.JDBC())
   val hikariDataSource = HikariDataSource(
      HikariConfig().apply {
         jdbcUrl = connectionString
         driverClassName = "org.sqlite.JDBC"
         isAutoCommit = false
      },
   )
   hikariDataSource.connection.use { conn ->
      conn.autoCommit = true
      statements.forEach {
         conn.createStatement().use { statement ->
            statement.execute(it)
         }
      }
   }
   return JdbcDataSource(hikariDataSource)
}
