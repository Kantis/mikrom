package io.github.kantis.mikrom.jdbc.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.PostgreSQLContainer

fun Spec.preparePostgresDatabase(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val ds = HikariDataSource(
      HikariConfig().apply {
         jdbcUrl = postgres.jdbcUrl
         username = postgres.username
         password = postgres.password
         isAutoCommit = false
      },
   )

   afterSpec {
      ds.close()
      postgres.stop()
   }

   ds.connection.use { conn ->
      conn.autoCommit = true
      statements.forEach { sql ->
         conn.createStatement().use { it.execute(sql) }
      }
   }

   return JdbcDataSource(ds)
}
