package io.github.kantis.mikrom.r2dbc

import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.suspend.SuspendingDataSource
import io.github.kantis.mikrom.suspend.SuspendingTransaction
import io.r2dbc.pool.ConnectionPool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

public class PooledR2dbcDataSource(private val underlyingConnectionPool: ConnectionPool) : SuspendingDataSource {
   private val driverConverters = r2dbcConverters(underlyingConnectionPool)
   private val placeholder = r2dbcPlaceholder(underlyingConnectionPool)

   override suspend fun <T> suspendingTransaction(block: suspend SuspendingTransaction.() -> T): T {
      val connection = underlyingConnectionPool.create().awaitSingle()

      // Commit, rollback and close must run even if the calling coroutine has been cancelled, or the
      // connection is never returned to the pool.
      try {
         connection.beginTransaction().awaitFirstOrNull()
         val result = R2dbcTransaction(connection, driverConverters, placeholder).block()

         withContext(NonCancellable) {
            when (result) {
               is Rollback -> {
                  logger.debug("Rolling back transaction, since transaction resulted in TransactionResult.Rollback")
                  connection.rollbackTransaction().awaitFirstOrNull()
               }

               else -> {
                  logger.debug("Committing transaction, since transaction resulted in TransactionResult.Commit")
                  connection.commitTransaction().awaitFirstOrNull()
               }
            }
         }

         return result
      } catch (t: Throwable) {
         if (t is CancellationException) {
            logger.debug("Rolling back transaction, since the coroutine was cancelled")
         } else {
            logger.warn("Rolling back transaction, since transaction resulted in unhandled exception: {}", t.message)
         }
         withContext(NonCancellable) {
            runCatching { connection.rollbackTransaction().awaitFirstOrNull() }
               .onFailure { t.addSuppressed(it) }
         }
         throw t
      } finally {
         withContext(NonCancellable) {
            logger.debug("Returning connection to the pool")
            connection.close().awaitFirstOrNull()
         }
      }
   }

   private companion object {
      private val logger = LoggerFactory.getLogger(PooledR2dbcDataSource::class.java)
   }
}
