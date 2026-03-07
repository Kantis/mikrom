package io.github.kantis.mikrom.r2dbc.mariadb

import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.testcontainers.containers.MariaDBContainer
import java.time.Duration

suspend fun prepareMariaDbDatabase(vararg statements: String): PooledR2dbcDataSource {
   val mariadb = MariaDBContainer<Nothing>("mariadb:11")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val connectionFactory = MariadbConnectionFactory(
      MariadbConnectionConfiguration.builder()
         .host(mariadb.host)
         .port(mariadb.getMappedPort(3306))
         .database(mariadb.databaseName)
         .username(mariadb.username)
         .password(mariadb.password)
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
