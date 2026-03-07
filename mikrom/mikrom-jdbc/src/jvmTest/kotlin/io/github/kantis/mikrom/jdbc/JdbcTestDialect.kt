package io.github.kantis.mikrom.jdbc

interface JdbcTestDialect {
   val name: String
   val supportsUuid: Boolean

   fun truncateTable(table: String): String = "TRUNCATE TABLE $table"

   fun createBooksTable(): String

   fun createTestRecordsTable(): String

   fun createDataTypesTable(): String

   fun createPeopleTable(): String

   fun insertBooks(): String = "INSERT INTO books (author, title, number_of_pages) VALUES (?, ?, ?)"

   fun insertTestRecord(): String = "INSERT INTO test_records (name) VALUES (?)"

   fun insertDataTypes(): String =
      """
      INSERT INTO data_types (
          string_field, int_field, long_field, boolean_field,
          double_field, decimal_field, date_field, timestamp_field${if (supportsUuid) ", uuid_field" else ""}
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?${if (supportsUuid) ", ?" else ""})
      """.trimIndent()

   fun insertPerson(): String = "INSERT INTO people (name, age) VALUES (?, ?)"
}

object H2JdbcDialect : JdbcTestDialect {
   override val name = "H2"
   override val supportsUuid = true

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

   override fun createPeopleTable() =
      """
      CREATE TABLE people (
          name VARCHAR(255),
          age INT
      )
      """.trimIndent()
}

object PostgresJdbcDialect : JdbcTestDialect {
   override val name = "PostgreSQL"
   override val supportsUuid = true

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

   override fun createPeopleTable() =
      """
      CREATE TABLE people (
          name VARCHAR(255),
          age INT
      )
      """.trimIndent()
}

object MySqlJdbcDialect : JdbcTestDialect {
   override val name = "MySQL"
   override val supportsUuid = false

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

   override fun createPeopleTable() =
      """
      CREATE TABLE people (
          name VARCHAR(255),
          age INT
      )
      """.trimIndent()
}

object MariaDbJdbcDialect : JdbcTestDialect {
   override val name = "MariaDB"
   override val supportsUuid = false

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

   override fun createPeopleTable() =
      """
      CREATE TABLE people (
          name VARCHAR(255),
          age INT
      )
      """.trimIndent()
}

object MssqlJdbcDialect : JdbcTestDialect {
   override val name = "MSSQL"
   override val supportsUuid = false

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

   override fun createPeopleTable() =
      """
      CREATE TABLE people (
          name VARCHAR(255),
          age INT
      )
      """.trimIndent()
}

object OracleJdbcDialect : JdbcTestDialect {
   override val name = "Oracle"
   override val supportsUuid = false

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

   override fun createPeopleTable() =
      """
      CREATE TABLE people (
          name VARCHAR2(255),
          age NUMBER(10)
      )
      """.trimIndent()
}
