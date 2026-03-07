package io.github.kantis.mikrom.r2dbc.mysql

import io.github.kantis.mikrom.r2dbc.MySqlDialect
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.dataTypeTests
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.github.kantis.mikrom.r2dbc.h2.prepareH2Database
import io.kotest.core.spec.style.FunSpec

class MySqlR2dbcTests : FunSpec(
   {
      val dialect = MySqlDialect
      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = prepareMySqlDatabase(
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
