package io.github.kantis.mikrom.r2dbc

interface R2dbcTestDialect {
   val name: String
   val supportsUuid: Boolean

   fun placeholder(position: Int): String

   fun truncateTable(table: String): String = "TRUNCATE TABLE $table"

   fun createBooksTable(): String

   fun createTestRecordsTable(): String

   fun createDataTypesTable(): String

   fun insertBooks(): String =
      "INSERT INTO books (author, title, number_of_pages) VALUES (${placeholder(1)}, ${placeholder(2)}, ${placeholder(3)})"

   fun insertTestRecord(): String = "INSERT INTO test_records (name) VALUES (${placeholder(1)})"

   fun insertDataTypes(): String =
      """
      INSERT INTO data_types (
          string_field, int_field, long_field, boolean_field,
          double_field, decimal_field, date_field, timestamp_field${if (supportsUuid) ", uuid_field" else ""}
      ) VALUES (${placeholder(
         1,
      )}, ${placeholder(
         2,
      )}, ${placeholder(
         3,
      )}, ${placeholder(
         4,
      )}, ${placeholder(5)}, ${placeholder(6)}, ${placeholder(7)}, ${placeholder(8)}${if (supportsUuid) ", ${placeholder(9)}" else ""})
      """.trimIndent()
}

object PostgresDialect : R2dbcTestDialect {
   override val name = "PostgreSQL"
   override val supportsUuid = true

   override fun placeholder(position: Int) = "$$position"

   override fun createBooksTable() =
      """
      CREATE TABLE books (
          author VARCHAR(255),
          title VARCHAR(255),
          number_of_pages INT
      )
      """.trimIndent()

   override fun createTestRecordsTable() =
      """
      CREATE TABLE test_records (
          id SERIAL PRIMARY KEY,
          name VARCHAR(255)
      )
      """.trimIndent()

   override fun createDataTypesTable() =
      """
      CREATE TABLE data_types (
          id SERIAL PRIMARY KEY,
          string_field VARCHAR(255),
          int_field INTEGER,
          long_field BIGINT,
          boolean_field BOOLEAN,
          double_field DOUBLE PRECISION,
          decimal_field DECIMAL(10,2),
          date_field DATE,
          timestamp_field TIMESTAMP,
          uuid_field UUID
      )
      """.trimIndent()
}

object H2Dialect : R2dbcTestDialect {
   override val name = "H2"
   override val supportsUuid = true

   override fun placeholder(position: Int) = "$$position"

   override fun createBooksTable() =
      """
      CREATE TABLE books (
          author VARCHAR(255),
          title VARCHAR(255),
          number_of_pages INT
      )
      """.trimIndent()

   override fun createTestRecordsTable() =
      """
      CREATE TABLE test_records (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255)
      )
      """.trimIndent()

   override fun createDataTypesTable() =
      """
      CREATE TABLE data_types (
          id INT AUTO_INCREMENT PRIMARY KEY,
          string_field VARCHAR(255),
          int_field INTEGER,
          long_field BIGINT,
          boolean_field BOOLEAN,
          double_field DOUBLE PRECISION,
          decimal_field DECIMAL(10,2),
          date_field DATE,
          timestamp_field TIMESTAMP,
          uuid_field UUID
      )
      """.trimIndent()
}

object MySqlDialect : R2dbcTestDialect {
   override val name = "MySQL"
   override val supportsUuid = false

   override fun placeholder(position: Int) = "?"

   override fun createBooksTable() =
      """
      CREATE TABLE books (
          author VARCHAR(255),
          title VARCHAR(255),
          number_of_pages INT
      )
      """.trimIndent()

   override fun createTestRecordsTable() =
      """
      CREATE TABLE test_records (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255)
      )
      """.trimIndent()

   override fun createDataTypesTable() =
      """
      CREATE TABLE data_types (
          id INT AUTO_INCREMENT PRIMARY KEY,
          string_field VARCHAR(255),
          int_field INTEGER,
          long_field BIGINT,
          boolean_field BOOLEAN,
          double_field DOUBLE PRECISION,
          decimal_field DECIMAL(10,2),
          date_field DATE,
          timestamp_field TIMESTAMP
      )
      """.trimIndent()
}

object MariaDbDialect : R2dbcTestDialect {
   override val name = "MariaDB"
   override val supportsUuid = false

   override fun placeholder(position: Int) = "?"

   override fun createBooksTable() =
      """
      CREATE TABLE books (
          author VARCHAR(255),
          title VARCHAR(255),
          number_of_pages INT
      )
      """.trimIndent()

   override fun createTestRecordsTable() =
      """
      CREATE TABLE test_records (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255)
      )
      """.trimIndent()

   override fun createDataTypesTable() =
      """
      CREATE TABLE data_types (
          id INT AUTO_INCREMENT PRIMARY KEY,
          string_field VARCHAR(255),
          int_field INTEGER,
          long_field BIGINT,
          boolean_field BOOLEAN,
          double_field DOUBLE PRECISION,
          decimal_field DECIMAL(10,2),
          date_field DATE,
          timestamp_field TIMESTAMP
      )
      """.trimIndent()
}

object MssqlDialect : R2dbcTestDialect {
   override val name = "MSSQL"
   override val supportsUuid = false

   override fun placeholder(position: Int) = "@P$position"

   override fun truncateTable(table: String) = "TRUNCATE TABLE $table"

   override fun createBooksTable() =
      """
      CREATE TABLE books (
          author VARCHAR(255),
          title VARCHAR(255),
          number_of_pages INT
      )
      """.trimIndent()

   override fun createTestRecordsTable() =
      """
      CREATE TABLE test_records (
          id INT IDENTITY(1,1) PRIMARY KEY,
          name VARCHAR(255)
      )
      """.trimIndent()

   override fun createDataTypesTable() =
      """
      CREATE TABLE data_types (
          id INT IDENTITY(1,1) PRIMARY KEY,
          string_field VARCHAR(255),
          int_field INTEGER,
          long_field BIGINT,
          boolean_field BIT,
          double_field FLOAT,
          decimal_field DECIMAL(10,2),
          date_field DATE,
          timestamp_field DATETIME2
      )
      """.trimIndent()
}

object OracleDialect : R2dbcTestDialect {
   override val name = "Oracle"
   override val supportsUuid = false

   override fun placeholder(position: Int) = ":$position"

   override fun truncateTable(table: String) = "TRUNCATE TABLE $table"

   override fun createBooksTable() =
      """
      CREATE TABLE books (
          author VARCHAR2(255),
          title VARCHAR2(255),
          number_of_pages NUMBER(10)
      )
      """.trimIndent()

   override fun createTestRecordsTable() =
      """
      CREATE TABLE test_records (
          id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
          name VARCHAR2(255)
      )
      """.trimIndent()

   override fun createDataTypesTable() =
      """
      CREATE TABLE data_types (
          id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
          string_field VARCHAR2(255),
          int_field NUMBER(10),
          long_field NUMBER(19),
          boolean_field NUMBER(1),
          double_field BINARY_DOUBLE,
          decimal_field NUMBER(10,2),
          date_field DATE,
          timestamp_field TIMESTAMP
      )
      """.trimIndent()
}
