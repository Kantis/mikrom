package io.github.kantis.mikrom.jdbc.mariadb

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.MariaDBContainer

fun Spec.prepareMariaDbDatabase(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val mariadb = MariaDBContainer<Nothing>("mariadb:11")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val ds = HikariDataSource(
      HikariConfig().apply {
         jdbcUrl = mariadb.jdbcUrl
         username = mariadb.username
         password = mariadb.password
         isAutoCommit = false
      },
   )

   afterSpec {
      ds.close()
      mariadb.stop()
   }

   ds.connection.use { conn ->
      conn.autoCommit = true
      statements.forEach { sql ->
         conn.createStatement().use { it.execute(sql) }
      }
   }

   return JdbcDataSource(ds)
}
