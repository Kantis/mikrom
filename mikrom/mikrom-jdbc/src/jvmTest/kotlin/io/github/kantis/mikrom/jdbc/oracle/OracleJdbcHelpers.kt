package io.github.kantis.mikrom.jdbc.oracle

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.OracleContainer

fun Spec.prepareOracleDatabase(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val oracle = OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val ds = HikariDataSource(
      HikariConfig().apply {
         jdbcUrl = oracle.jdbcUrl
         username = oracle.username
         password = oracle.password
         isAutoCommit = false
      },
   )

   afterSpec {
      ds.close()
      oracle.stop()
   }

   ds.connection.use { conn ->
      conn.autoCommit = true
      statements.forEach { sql ->
         conn.createStatement().use { it.execute(sql) }
      }
   }

   return JdbcDataSource(ds)
}
