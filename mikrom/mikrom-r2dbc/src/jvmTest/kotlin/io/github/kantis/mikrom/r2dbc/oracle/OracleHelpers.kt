package io.github.kantis.mikrom.r2dbc.oracle

import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.testcontainers.containers.OracleContainer
import java.time.Duration

suspend fun prepareOracleDatabase(vararg statements: String): PooledR2dbcDataSource {
   val oracle = OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
      .apply {
         withDatabaseName("testdb")
         withUsername("testuser")
         withPassword("testpass")
         start()
      }

   val r2dbcUrl = "r2dbc:oracle://${oracle.host}:${oracle.oraclePort}/${oracle.databaseName}"
   val connectionFactory = ConnectionFactories.get(
      ConnectionFactoryOptions.parse(r2dbcUrl)
         .mutate()
         .option(ConnectionFactoryOptions.USER, oracle.username)
         .option(ConnectionFactoryOptions.PASSWORD, oracle.password)
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
