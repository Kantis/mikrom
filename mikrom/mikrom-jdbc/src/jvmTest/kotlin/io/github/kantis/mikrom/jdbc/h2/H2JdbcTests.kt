package io.github.kantis.mikrom.jdbc.h2

import io.github.kantis.mikrom.jdbc.H2JdbcDialect
import io.github.kantis.mikrom.jdbc.JdbcDataSource
import io.github.kantis.mikrom.jdbc.factories.basicInsertQueryTests
import io.github.kantis.mikrom.jdbc.factories.dataTypeTests
import io.github.kantis.mikrom.jdbc.factories.namedParamTests
import io.github.kantis.mikrom.jdbc.factories.parameterMapperTests
import io.github.kantis.mikrom.jdbc.factories.queryingForPrimitivesTests
import io.github.kantis.mikrom.jdbc.factories.transactionTests
import io.kotest.core.spec.style.FunSpec

class H2JdbcTests : FunSpec(
   {
      val dialect = H2JdbcDialect
      lateinit var dataSource: JdbcDataSource

      beforeSpec {
         dataSource = prepareH2Database(
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
