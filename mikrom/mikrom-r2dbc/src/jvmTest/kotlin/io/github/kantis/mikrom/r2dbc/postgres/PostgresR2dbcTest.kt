package io.github.kantis.mikrom.r2dbc.postgres

import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.PostgresDialect
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.dataTypeTests
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.github.kantis.mikrom.r2dbc.h2.prepareH2Database
import io.kotest.core.spec.style.FunSpec

class PostgresR2dbcTest : FunSpec(
   {
      val dialect = PostgresDialect
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
//      include(basicInsertQueryTests(dialect, streaming = false, dataSourceProvider))
      include(transactionTests(dialect, streaming = true, dataSourceProvider))
//      include(transactionTests(dialect, streaming = false, dataSourceProvider))
//      include(dataTypeTests(dialect, dataSourceProvider))
   },
)
