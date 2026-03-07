package io.github.kantis.mikrom.r2dbc.mariadb

import io.github.kantis.mikrom.r2dbc.MariaDbDialect
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.dataTypeTests
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.kotest.core.spec.style.FunSpec

class MariaDbR2dbcTests : FunSpec(
   {
      val dialect = MariaDbDialect
      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = prepareMariaDbDatabase(
            dialect.createBooksTable(),
            dialect.createTestRecordsTable(),
            dialect.createDataTypesTable(),
         )
      }

      val dataSourceProvider = { dataSource }

//      include(dataTypeTests(dialect, dataSourceProvider))
      include(basicInsertQueryTests(dialect, streaming = true, dataSourceProvider))
//      include(basicInsertQueryTests(dialect, streaming = false, dataSourceProvider))
      include(transactionTests(dialect, streaming = true, dataSourceProvider))
//      include(transactionTests(dialect, streaming = false, dataSourceProvider))
   },
)
