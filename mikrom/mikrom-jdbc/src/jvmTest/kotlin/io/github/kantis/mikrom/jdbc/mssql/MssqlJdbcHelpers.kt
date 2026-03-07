package io.github.kantis.mikrom.jdbc.mssql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.MSSQLServerContainer

fun Spec.prepareMssqlDatabase(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val mssql = MSSQLServerContainer<Nothing>("mcr.microsoft.com/mssql/server:2022-latest")
      .apply {
         acceptLicense()
         start()
      }

   val ds = HikariDataSource(
      HikariConfig().apply {
         jdbcUrl = mssql.jdbcUrl
         username = mssql.username
         password = mssql.password
         isAutoCommit = false
      },
   )

   afterSpec {
      ds.close()
      mssql.stop()
   }

   ds.connection.use { conn ->
      conn.autoCommit = true
      statements.forEach { sql ->
         conn.createStatement().use { it.execute(sql) }
      }
   }

   return JdbcDataSource(ds)
}
