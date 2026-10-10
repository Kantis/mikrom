# Getting Started

## JDBC with Compiler plugin
Mikrom comes with a compiler plugin that will generate `RowMapper` and `ParameterMapper` implementations for you. This means that you
can use standard data classes to create ergonomic, yet type-safe, queries.

Add the Mikrom dependency and Gradle plugin to your project:
```kotlin
plugins {
  id("io.github.kantis.mikrom") version "0.7.0"
}

dependencies {
  implementation("io.github.kantis:mikrom-jdbc:0.7.0")
}
```

Then, all you need is to create a JDBC connection (`javax.sql.DataSource`), a Mikrom `DataSource` and a `Mikrom` instance:


```kotlin

@JvmInline
value class BookTitle(val title: String)

@MikromResult
data class SavedBook(
  val id: Int,
  val author: String,
  val title: BookTitle,  // Value classes are supported out of the box
  val numberOfPages: Int
)

fun main() {
  val dbConnection = DriverManager.getConnection("jdbc:your_database_url")
  val datasource = JdbcDataSource(dbConnection) // Note: you can pass a HikariCP data source here, if you want connection pooling.
  val mikrom = Mikrom {
    // configure your Mikrom instance here.
  }
}
```

### Querying the database
Simply write your SQL, and use the `Mikrom.queryFor` method to execute it.
In order to query for data classes, a `RowMapper` must be registered for that type.

```kotlin
dataSource.transaction {
  mikrom.queryFor<Book>("SELECT * FROM books")
}
```

To use a `where`-statement, just add the parameter. Parameters can be passed either as named parameters or as positional parameters.

```kotlin
dataSource.transaction {
  // using named parameters
  mikrom.queryFor<Book>(
    "SELECT * FROM books WHERE number_of_pages > :minPages",
    mapOf("minPages" to 200)
  )

  // or using positional parameters
  mikrom.queryFor<Book>(
    "SELECT * FROM books WHERE number_of_pages > ?", 200
  )
}
```

### Inserting data
Use the `Mikrom.execute` method to insert data.

```kotlin
@MikromParameter
data class CreateBookCommand(val author: String, val title: BookTitle, val numberOfPages: Int)

dataSource.transaction {
  // Using named parameters with a generated ParameterMapper
  mikrom.execute(
    """
        INSERT INTO books(author, title, number_of_pages)
        VALUES (:author, :title, :numberOfPages)
    """,
    CreateBookCommand("JRR Tolkien", BookTitle("The Hobbit"), 310),
    CreateBookCommand("George Orwell", BookTitle("1984"), 328),
  )

  // or, using positional parameters
  mikrom.execute(
    "INSERT INTO books(author, title, number_of_pages) VALUES (?, ?, ?)",
    listOf("JRR Tolkien", "The Hobbit", 310),
    listOf("George Orwell", "1984", 328),
  )
}
```

### Transactions

All database operations must be wrapped in a transaction. The `execute` and `queryFor` methods are only
defined in the context of a transaction, so attempting to use them without one will result in a compilation error.

```kotlin
dataSource.transaction {
  mikrom.execute(
    "INSERT INTO books (author, title, number_of_pages) VALUES (:author, :title, :numberOfPages)",
    Book("JRR Tolkien", BookTitle("The Hobbit"), 310),
    Book("George Orwell", BookTitle("1984"), 328),
  )

  mikrom.queryFor<Book>("SELECT * FROM books") shouldBe
    listOf(
      Book("JRR Tolkien", "The Hobbit", 310),
      Book("George Orwell", "1984", 328),
    )
}
```

## R2DBC with coroutines

`mikrom-r2dbc` exposes the same API as suspending functions, on top of an R2DBC `ConnectionPool`.
The compiler plugin, `RowMapper`s, `ParameterMapper`s and converters are shared with JDBC.

```kotlin
dependencies {
  implementation("io.github.kantis:mikrom-r2dbc:0.7.0")
}
```

Create an `io.r2dbc.pool.ConnectionPool` for your driver and wrap it in a `PooledR2dbcDataSource`:

```kotlin
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.queryFor

val connectionFactory = PostgresqlConnectionFactory(
  PostgresqlConnectionConfiguration.builder()
    .host("localhost")
    .database("books")
    .username("user")
    .password("secret")
    .build(),
)
val pool = ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory).maxSize(10).build())

val dataSource = PooledR2dbcDataSource(pool)
val mikrom = Mikrom {}
```

!!! note "Imports"
    The suspending `execute` and `queryFor` live in the `io.github.kantis.mikrom.suspend` package, so they can
    coexist with the blocking ones. Make sure you import the right variant.

### Transactions

Every operation runs inside `suspendingTransaction { }`. The block commits when it returns normally, rolls back when
it throws or when it returns the `Rollback` marker, and always hands the connection back to the pool afterwards.

```kotlin
dataSource.suspendingTransaction {
  mikrom.execute(
    "INSERT INTO books (author, title, number_of_pages) VALUES (:author, :title, :numberOfPages)",
    CreateBookCommand("JRR Tolkien", BookTitle("The Hobbit"), 310),
    CreateBookCommand("George Orwell", BookTitle("1984"), 328),
  )

  val longReads = mikrom
    .queryFor<Book>("SELECT * FROM books WHERE number_of_pages > :minPages", mapOf("minPages" to 320))
    .toList()
}
```

### Queries return a `Flow`

The suspending `queryFor` returns a cold `Flow<T>`. Nothing is sent to the database until the flow is collected, and
collecting it needs the transaction's connection.

!!! warning "Collect inside the transaction"
    Collect the flow (`toList()`, `first()`, `collect { }` …) **before the `suspendingTransaction` block returns**.
    A flow that escapes the block fails with `The connection is closed` when it is eventually collected, because the
    connection has already been returned to the pool.

```kotlin
// OK: the list is built while the transaction is open
val books = dataSource.suspendingTransaction {
  mikrom.queryFor<Book>("SELECT * FROM books").toList()
}

// Broken: the flow is collected after the connection was released
val flow = dataSource.suspendingTransaction {
  mikrom.queryFor<Book>("SELECT * FROM books")
}
flow.toList() // IllegalStateException: The connection is closed
```

### Parameters

Parameters work the same way as with JDBC: a `List` binds positionally, a `Map<String, Any?>` resolves `:named`
parameters, and a `@MikromParameter` class (or a type with a registered `ParameterMapper`) is mapped to named
parameters.

Named parameters are portable. Mikrom rewrites them to whatever positional syntax the driver expects
(`$1` for PostgreSQL, `@P1` for SQL Server, `?` for the rest), so `:minPages` works unchanged across databases.

Positional parameters are passed through to the driver as written, so use the driver's own placeholder syntax:

```kotlin
mikrom.queryFor<Book>("SELECT * FROM books WHERE number_of_pages > $1", listOf(320))  // PostgreSQL, H2
mikrom.queryFor<Book>("SELECT * FROM books WHERE number_of_pages > ?", listOf(320))   // MySQL, MariaDB
mikrom.queryFor<Book>("SELECT * FROM books WHERE number_of_pages > @P1", listOf(320)) // SQL Server
```

!!! warning "Nulls need a type"
    R2DBC drivers must know the SQL type of a `NULL` parameter, so a bare `null` in a parameter list or map is
    rejected. Pass `TypedNull(Int::class)` instead. `@MikromParameter` classes do this automatically for nullable
    properties, see [Parameter Types](parameter-types.md).

### Streaming parameters

For large inserts, `executeStreaming` accepts the parameter sets as a `Flow<List<Any>>` and returns a `Job`.
The job runs concurrently with the rest of the block, so `join()` it before the transaction block returns, otherwise
the transaction commits while inserts are still in flight.

```kotlin
dataSource.suspendingTransaction {
  mikrom.executeStreaming(
    "INSERT INTO books (author, title, number_of_pages) VALUES ($1, $2, $3)",
    booksToImport.map { listOf(it.author, it.title, it.numberOfPages) },
  ).join()
}
```
