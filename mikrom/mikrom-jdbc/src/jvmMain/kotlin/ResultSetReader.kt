package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.MikromInternal
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.buildRow
import io.github.kantis.mikrom.convert.TypeConverters
import java.sql.ResultSet
import java.sql.Types

@MikromInternal
public object ResultSetReader {
   @MikromInternal
   public fun loadResultSet(
      resultSet: ResultSet,
      driverConverters: TypeConverters = TypeConverters.EMPTY,
   ): List<Row> {
      val rows = mutableListOf<Row>()
      try {
         while (resultSet.next()) {
            rows.add(loadRow(resultSet, driverConverters))
         }
      } finally {
         resultSet.close()
      }
      return rows
   }

   private fun loadRow(
      resultSet: ResultSet,
      driverConverters: TypeConverters,
   ): Row {
      val metaData = resultSet.metaData
      return buildRow(driverConverters) {
         for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnLabel(i).lowercase()
            val columnType = metaData.getColumnType(i)
            val sqlTypeName = metaData.getColumnTypeName(i)
            val value = when (columnType) {
               Types.BIT -> resultSet.getBoolean(i)

               Types.TINYINT,
               Types.SMALLINT,
               Types.INTEGER,
               -> resultSet.getInt(i)

               Types.BIGINT -> resultSet.getLong(i)

               Types.FLOAT,
               Types.DOUBLE,
               Types.REAL,
               -> resultSet.getDouble(i)

               Types.NUMERIC,
               Types.DECIMAL,
               -> resultSet.getBigDecimal(i)

               Types.CHAR,
               Types.VARCHAR,
               Types.LONGVARCHAR,
               Types.NCHAR,
               Types.NVARCHAR,
               Types.LONGNVARCHAR,
               -> resultSet.getString(i)

               Types.BOOLEAN -> resultSet.getBoolean(i)

               Types.DATE -> resultSet.getDate(i)

               Types.TIME -> resultSet.getTime(i)

               Types.TIME_WITH_TIMEZONE -> TODO("TIME_WITH_TIMEZONE is not supported yet")

               Types.TIMESTAMP -> resultSet.getTimestamp(i)

               Types.TIMESTAMP_WITH_TIMEZONE -> TODO("TIMESTAMP_WITH_TIMEZONE is not supported yet")

               Types.BINARY,
               Types.VARBINARY,
               Types.LONGVARBINARY,
               -> resultSet.getBytes(i)

               else -> resultSet.getObject(i)
            }

            val resolvedValue = if (resultSet.wasNull()) null else value

            val kotlinType = when (columnType) {
               Types.BIT, Types.BOOLEAN -> Boolean::class

               Types.TINYINT, Types.SMALLINT, Types.INTEGER -> Int::class

               Types.BIGINT -> Long::class

               Types.FLOAT, Types.DOUBLE, Types.REAL -> Double::class

               Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
               Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
               -> String::class

               Types.TIMESTAMP -> java.sql.Timestamp::class

               else -> null
            }

            column(columnName, resolvedValue, kotlinType, sqlTypeName)
         }
      }
   }
}
