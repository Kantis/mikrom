package io.github.kantis.mikrom.r2dbc.mysql

import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.testcontainers.containers.MySQLContainer
import java.time.Duration

suspend fun prepareMySqlDatabase(vararg statements: String): PooledR2dbcDataSource {
   val mysql = MySQLContainer<Nothing>("mysql:8.0")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val connectionFactory = MySqlConnectionFactory.from(
      MySqlConnectionConfiguration.builder()
         .host(mysql.host)
         .port(mysql.getMappedPort(3306))
         .database(mysql.databaseName)
         .user(mysql.username)
         .password(mysql.password)
         .build(),
   )

   val poolConfiguration = ConnectionPoolConfiguration.builder()
      .connectionFactory(connectionFactory)
      .maxIdleTime(Duration.ofMinutes(30))
      .maxSize(20)
      .build()

   val pool = ConnectionPool(poolConfiguration)

   val connection = requireNotNull(pool.create().awaitSingle())
   connection.isAutoCommit = false
   connection.beginTransaction().awaitFirstOrNull()

   try {
      statements.forEach { statement ->
         connection.createStatement(statement).execute().awaitSingle()
      }
      connection.commitTransaction().awaitFirstOrNull()
   } catch (e: Exception) {
      connection.rollbackTransaction().awaitFirstOrNull()
      throw e
   } finally {
      connection.close().awaitFirstOrNull()
   }

   return PooledR2dbcDataSource(pool)
}
