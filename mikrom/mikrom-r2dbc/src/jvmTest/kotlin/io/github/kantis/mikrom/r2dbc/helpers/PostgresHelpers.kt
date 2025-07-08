package io.github.kantis.mikrom.r2dbc.helpers

import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.PostgreSQLContainer

suspend fun preparePostgresDatabase(
   @Language("SQL")vararg statements: String,
): PooledR2dbcDataSource {
   val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val connectionFactory = PostgresqlConnectionFactory(
      PostgresqlConnectionConfiguration.builder()
         .host(postgres.host)
         .port(postgres.getMappedPort(5432))
         .database(postgres.databaseName)
         .username(postgres.username)
         .password(postgres.password)
         .build(),
   )

   val poolConfiguration = ConnectionPoolConfiguration.builder()
      .connectionFactory(connectionFactory)
      .maxIdleTime(java.time.Duration.ofMinutes(30))
      .maxSize(20)
      .build()

   val pool = ConnectionPool(poolConfiguration)

   // Execute setup statements
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
