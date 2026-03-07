package io.github.kantis.mikrom.r2dbc.oracle

import io.github.kantis.mikrom.r2dbc.OracleDialect
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.github.kantis.mikrom.r2dbc.h2.prepareH2Database
import io.kotest.core.spec.style.FunSpec

class OracleR2dbcTests : FunSpec(
   {
      val dialect = OracleDialect
      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = prepareOracleDatabase(
            dialect.createBooksTable(),
            dialect.createTestRecordsTable(),
            dialect.createDataTypesTable(),
         )
      }

      val dataSourceProvider = { dataSource }

//      include(basicInsertQueryTests(dialect, streaming = true, dataSourceProvider))
//      include(transactionTests(dialect, streaming = true, dataSourceProvider))
//      include(dataTypeTests(dialect, dataSourceProvider))
   },
)
