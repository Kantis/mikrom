package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.Rollback
import io.github.kantis.mikrom.datasource.DataSource
import io.github.kantis.mikrom.datasource.Transaction
import org.slf4j.LoggerFactory

public class JdbcDataSource(private val underlyingDataSource: javax.sql.DataSource) : DataSource {
   override fun <T> transaction(block: Transaction.() -> T): T {
      underlyingDataSource.connection.use { jdbcConnection ->
         jdbcConnection.autoCommit = false
         jdbcConnection.beginRequest()
         return try {
            val transaction = JdbcTransaction(jdbcConnection)
            val result = transaction.block()

            when (result) {
               is Rollback -> {
                  logger.info("Rolling back transaction, since transaction resulted in TransactionResult.Rollback")
                  jdbcConnection.rollback()
               }

               else -> {
                  logger.info("Committing transaction, since transaction resulted in TransactionResult.Commit")
                  jdbcConnection.commit()
               }
            }

            result
         } catch (e: Exception) {
            logger.warn("Rolling back transaction, since transaction resulted in unhandled exception: {}", e.message)
            jdbcConnection.rollback()
            throw e
         } finally {
            logger.debug("Closing JDBC request")
            jdbcConnection.endRequest()
         }
      }
   }

   public companion object {
      private val logger = LoggerFactory.getLogger(JdbcDataSource::class.java)
   }
}
