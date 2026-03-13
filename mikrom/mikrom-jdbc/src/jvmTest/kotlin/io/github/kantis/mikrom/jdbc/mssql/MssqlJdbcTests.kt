package io.github.kantis.mikrom.jdbc.mssql

import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.MssqlJdbcDialect
import io.github.kantis.mikrom.jdbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.jdbc.factories.dataTypeTests
import io.github.kantis.mikrom.jdbc.factories.namedParamTests
import io.github.kantis.mikrom.jdbc.factories.parameterMapperTests
import io.github.kantis.mikrom.jdbc.factories.queryingForPrimitivesTests
import io.github.kantis.mikrom.jdbc.factories.transactionTests
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec

@Tags("mssql")
class MssqlJdbcTests : FunSpec(
   {
      val dialect = MssqlJdbcDialect
      lateinit var dataSource: JdbcDataSource

      beforeSpec {
         dataSource = prepareMssqlDatabase(
            dialect.createBooksTable(),
            dialect.createTestRecordsTable(),
            dialect.createDataTypesTable(),
            dialect.createPeopleTable(),
         )
      }

      val dataSourceProvider = { dataSource }

      include(basicInsertQueryTests(dialect, dataSourceProvider))
      include(transactionTests(dialect, dataSourceProvider))
      include(dataTypeTests(dialect, dataSourceProvider))
      include(namedParamTests(dialect, dataSourceProvider))
      include(parameterMapperTests(dialect, dataSourceProvider))
      include(queryingForPrimitivesTests(dialect, dataSourceProvider))
   },
)
