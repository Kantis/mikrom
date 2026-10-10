package io.github.kantis.mikrom.r2dbc.h2

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.get
import io.github.kantis.mikrom.r2dbc.H2Dialect
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private data class TestRecord(val id: Int, val name: String)

/**
 * Cancellation handling does not depend on the database, so it is only verified against H2.
 */
@Tags("h2")
class CancellationTests : FunSpec(
   {
      val dialect = H2Dialect
      val mikrom = Mikrom {
         registerRowMapper { row -> TestRecord(row.get("id"), row.get("name")) }
      }

      test("cancelling a transaction rolls it back and returns the connection to the pool") {
         // A pool of one makes a leaked connection impossible to miss: the next transaction would hang.
         val pool = ConnectionPool(
            ConnectionPoolConfiguration.builder()
               .connectionFactory(
                  H2ConnectionFactory(H2ConnectionConfiguration.builder().inMemory("cancel-${UUID.randomUUID()}").build()),
               )
               .maxSize(1)
               .build(),
         )
         val dataSource = PooledR2dbcDataSource(pool)
         dataSource.suspendingTransaction { mikrom.execute(dialect.createTestRecordsTable()) }

         val inserted = CompletableDeferred<Unit>()
         val job = launch {
            dataSource.suspendingTransaction {
               mikrom.execute(dialect.insertTestRecord(), listOf("doomed"))
               inserted.complete(Unit)
               awaitCancellation()
            }
         }
         inserted.await()
         job.cancelAndJoin()

         pool.metrics.get().acquiredSize() shouldBe 0

         withTimeout(5.seconds) {
            dataSource.suspendingTransaction {
               mikrom.queryFor<TestRecord>("SELECT * FROM test_records").shouldContainExactly()
            }
         }
      }
   },
)
