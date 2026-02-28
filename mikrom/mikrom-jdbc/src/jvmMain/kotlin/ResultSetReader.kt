package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.MikromInternal
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.buildRow
import java.sql.ResultSet
import java.sql.Types

@MikromInternal
public object ResultSetReader {
   @MikromInternal
   public fun loadResultSet(resultSet: ResultSet): List<Row> {
      val rows = mutableListOf<Row>()
      try {
         while (resultSet.next()) {
            rows.add(loadRow(resultSet))
         }
      } finally {
         resultSet.close()
      }
      return rows
   }

   private fun loadRow(resultSet: ResultSet): Row {
      val metaData = resultSet.metaData
      return buildRow {
         for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnType(i)
            val sqlTypeName = metaData.getColumnTypeName(i)
            val value = when (columnType) {
               Types.BIT,
               Types.TINYINT,
               Types.SMALLINT,
               Types.INTEGER,
               Types.BIGINT,
               -> resultSet.getInt(i)

               Types.DECIMAL -> resultSet.getBigDecimal(i)

               Types.VARCHAR -> resultSet.getString(i)

               Types.BOOLEAN -> resultSet.getBoolean(i)

               Types.DATE -> resultSet.getDate(i)

               Types.TIME -> resultSet.getTime(i)

               Types.TIME_WITH_TIMEZONE -> TODO("TIME_WITH_TIMEZONE is not supported yet")

               Types.TIMESTAMP -> resultSet.getTimestamp(i)

               Types.TIMESTAMP_WITH_TIMEZONE -> TODO("TIMESTAMP_WITH_TIMEZONE is not supported yet")

               else -> error("Column $i is of unsupported type $columnType")
            }

            val kotlinType = when (columnType) {
               Types.BIT, Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> Int::class
               Types.VARCHAR -> String::class
               Types.BOOLEAN -> Boolean::class
               Types.TIMESTAMP -> java.sql.Timestamp::class
               else -> null
            }

            column(columnName, value, kotlinType, sqlTypeName)
         }
      }
   }
}
