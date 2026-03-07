package io.github.kantis.mikrom.jdbc

import io.github.kantis.mikrom.AnsiString
import io.github.kantis.mikrom.Query
import io.github.kantis.mikrom.Row
import io.github.kantis.mikrom.TypedNull
import io.github.kantis.mikrom.convert.TypeConversions
import io.github.kantis.mikrom.datasource.Transaction
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

public class JdbcTransaction(
   private val connection: Connection,
   private val driverConversions: TypeConversions = TypeConversions.EMPTY,
) : Transaction {
   override fun executeInTransaction(
      query: Query,
      vararg params: List<*>,
   ) {
      connection.prepareStatement(query).use { statement ->
         params.forEach { params ->
            bindParameters(statement, params)
            statement.execute()
         }
      }
   }

   override fun query(
      query: Query,
      params: List<*>,
   ): List<Row> =
      connection.prepareStatement(query).use { statement ->
         bindParameters(statement, params)
         statement.executeQuery().let { ResultSetReader.loadResultSet(it, driverConversions) }
      }

   private fun bindParameters(
      statement: PreparedStatement,
      params: List<*>,
   ) {
      val statementParams = statement.parameterMetaData
      require(params.count() == statementParams.parameterCount) {
         "Expected ${statementParams.parameterCount} parameters, but got ${params.count()}"
      }

      params.forEachIndexed { index, param ->
         when (param) {
            is AnsiString -> statement.setObject(index + 1, param.value, Types.VARCHAR)
            is String -> statement.setString(index + 1, param)
            is Int -> statement.setInt(index + 1, param)
            is Long -> statement.setLong(index + 1, param)
            is Double -> statement.setDouble(index + 1, param)
            is Float -> statement.setFloat(index + 1, param)
            is Boolean -> statement.setBoolean(index + 1, param)
            is ByteArray -> statement.setBytes(index + 1, param)
            is LocalDate -> statement.setObject(index + 1, param)
            is LocalDateTime -> statement.setTimestamp(index + 1, Timestamp.valueOf(param))
            is BigDecimal -> statement.setBigDecimal(index + 1, param)
            is Instant -> statement.setTimestamp(index + 1, Timestamp.from(param))
            is UUID -> statement.setObject(index + 1, param)
            is TypedNull -> statement.setNull(index + 1, statementParams.getParameterType(index + 1))
            null -> statement.setNull(index + 1, statementParams.getParameterType(index + 1))
            else -> error("Unsupported parameter type: ${param::class.simpleName} at index ${index + 1} with value $param")
         }
      }
   }
}
