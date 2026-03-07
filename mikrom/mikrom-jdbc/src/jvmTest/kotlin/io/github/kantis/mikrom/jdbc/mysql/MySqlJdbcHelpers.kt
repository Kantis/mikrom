package io.github.kantis.mikrom.jdbc.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.MySQLContainer

fun Spec.prepareMySqlDatabase(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val mysql = MySQLContainer<Nothing>("mysql:8.0")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val ds = HikariDataSource(
      HikariConfig().apply {
         jdbcUrl = mysql.jdbcUrl
         username = mysql.username
         password = mysql.password
         isAutoCommit = false
      },
   )

   afterSpec {
      ds.close()
      mysql.stop()
   }

   ds.connection.use { conn ->
      conn.autoCommit = true
      statements.forEach { sql ->
         conn.createStatement().use { it.execute(sql) }
      }
   }

   return JdbcDataSource(ds)
}
