package io.github.kantis.mikrom.r2dbc.postgres

import io.github.kantis.mikrom.r2dbc.PooledR2dbcDataSource
import io.github.kantis.mikrom.r2dbc.PostgresDialect
import io.github.kantis.mikrom.r2dbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.r2dbc.factories.dataTypeTests
import io.github.kantis.mikrom.r2dbc.factories.namedParameterTests
import io.github.kantis.mikrom.r2dbc.factories.parameterMapperTests
import io.github.kantis.mikrom.r2dbc.factories.queryingForPrimitivesTests
import io.github.kantis.mikrom.r2dbc.factories.transactionTests
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec

@Tags("postgres")
class PostgresR2dbcTest : FunSpec(
   {
      val dialect = PostgresDialect
      lateinit var dataSource: PooledR2dbcDataSource

      beforeSpec {
         dataSource = preparePostgresDatabase(
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
