package io.github.kantis.mikrom.jdbc.mariadb

import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.MariaDbJdbcDialect
import io.github.kantis.mikrom.jdbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.jdbc.factories.dataTypeTests
import io.github.kantis.mikrom.jdbc.factories.namedParamTests
import io.github.kantis.mikrom.jdbc.factories.parameterMapperTests
import io.github.kantis.mikrom.jdbc.factories.queryingForPrimitivesTests
import io.github.kantis.mikrom.jdbc.factories.transactionTests
import io.kotest.core.spec.style.FunSpec

class MariaDbJdbcTests : FunSpec(
   {
      val dialect = MariaDbJdbcDialect
      lateinit var dataSource: JdbcDataSource

      beforeSpec {
         dataSource = prepareMariaDbDatabase(
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
