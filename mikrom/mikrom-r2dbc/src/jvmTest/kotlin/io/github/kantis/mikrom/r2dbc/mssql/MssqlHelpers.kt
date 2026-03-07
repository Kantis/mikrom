package io.github.kantis.mikrom.r2dbc.mssql

import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.r2dbc.mssql.MssqlConnectionConfiguration
import io.r2dbc.mssql.MssqlConnectionFactory
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.testcontainers.containers.MSSQLServerContainer
import java.time.Duration

suspend fun prepareMssqlDatabase(vararg statements: String): PooledR2dbcDataSource {
   val mssql = MSSQLServerContainer<Nothing>("mcr.microsoft.com/mssql/server:2022-latest")
      .apply {
         acceptLicense()
         start()
      }

   val connectionFactory = MssqlConnectionFactory(
      MssqlConnectionConfiguration.builder()
         .host(mssql.host)
         .port(mssql.getMappedPort(1433))
         .database("master")
         .username(mssql.username)
         .password(mssql.password)
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
