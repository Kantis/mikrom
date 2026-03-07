package io.github.kantis.mikrom.jdbc.h2

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.kotest.core.spec.Spec
import org.h2.Driver
import org.intellij.lang.annotations.Language
import java.sql.DriverManager

fun Spec.prepareH2Database(
   @Language("SQL") vararg statements: String,
): JdbcDataSource {
   val connectionString = "jdbc:h2:mem:${this::class.simpleName?.lowercase() ?: error("Unknown spec")};" +
      "IGNORECASE=true;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;"
   DriverManager.registerDriver(Driver())
   val jdbcConnection = DriverManager.getConnection(connectionString)
   jdbcConnection.autoCommit = false
   jdbcConnection.beginRequest()
   statements.forEach {
      jdbcConnection.createStatement().use { statement ->
         statement.execute(it)
      }
   }
   jdbcConnection.commit()
   jdbcConnection.endRequest()
   return JdbcDataSource(
      HikariDataSource(
         HikariConfig().apply {
            jdbcUrl = connectionString
            driverClassName = "org.h2.Driver"
            isAutoCommit = false
         },
      ),
   )
}
