package io.github.kantis.mikrom.r2dbc.oracle

import io.github.kantis.mikrom.r2dbc.OracleDialect
import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.dataTypeTests
import io.github.kantis.mikrom.r2dbc.factories.namedParameterTests
import io.github.kantis.mikrom.r2dbc.factories.parameterMapperTests
import io.github.kantis.mikrom.r2dbc.factories.queryingForPrimitivesTests
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec

@Tags("oracle")
class OracleR2dbcTests : FunSpec(
   {
      val dialect = OracleDialect
      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = prepareOracleDatabase(
            dialect.createBooksTable(),
            dialect.createTestRecordsTable(),
            dialect.createDataTypesTable(),
            dialect.createPeopleTable(),
         )
      }

      val dataSourceProvider = { dataSource }

      include(basicInsertQueryTests(dialect, streaming = true, dataSourceProvider))
      include(basicInsertQueryTests(dialect, streaming = false, dataSourceProvider))
      include(transactionTests(dialect, streaming = true, dataSourceProvider))
      include(transactionTests(dialect, streaming = false, dataSourceProvider))
      include(dataTypeTests(dialect, dataSourceProvider))
      include(namedParameterTests(dialect, dataSourceProvider))
      include(parameterMapperTests(dialect, dataSourceProvider))
      include(queryingForPrimitivesTests(dialect, dataSourceProvider))
   },
)
