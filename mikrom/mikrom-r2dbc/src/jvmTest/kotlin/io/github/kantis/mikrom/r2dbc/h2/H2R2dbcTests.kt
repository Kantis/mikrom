package io.github.kantis.mikrom.r2dbc.h2

import io.github.kantis.mikrom.r2dbc.H2Dialect
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.dataTypeTests
import io.github.kantis.mikrom.r2dbc.factories.streamingTestName
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec

@Tags("h2")
class H2R2dbcTests : FunSpec(
   {
      val dialect = H2Dialect
      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = prepareH2Database(
            dialect.createBooksTable(),
            dialect.createTestRecordsTable(),
            dialect.createDataTypesTable(),
         )
      }

      val dataSourceProvider = { dataSource }

      include(basicInsertQueryTests(dialect, streaming = true, dataSourceProvider))
      include(basicInsertQueryTests(dialect, streaming = false, dataSourceProvider))
      include(transactionTests(dialect, streaming = true, dataSourceProvider))
      include(transactionTests(dialect, streaming = false, dataSourceProvider))
      include(dataTypeTests(dialect, dataSourceProvider))
   },
)
