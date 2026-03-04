# Getting Started

## JDBC with Compiler plugin
Mikrom comes with a compiler plugin that will generate `RowMapper` and `ParameterMapper` implementations for you. This means that you
can use standard data classes to create ergonomic, yet type-safe, queries.

Add the Mikrom dependency and Gradle plugin to your project:
```kotlin
plugins {
  id("io.github.kantis.mikrom") version "$VERSION"
}

dependencies {
  implementation("io.github.kantis.mikrom:mikrom-jdbc:$VERSION")
}
```

Then, all you need is to create a JDBC connection (`javax.sql.DataSource`), a Mikrom `DataSource` and a `Mikrom` instance:


```kotlin

@JvmInline
value class BookTitle(val title: String)

@RowMapped
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
dataSource.transation {
  mikrom.queryFor<Book>("SELECT * FROM books")
}
```

To use a `where`-statement, just add the parameter. Parameters can be passed either as named parameters or as positional parameters.

```kotlin
dataSource.transation {
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
@ParameterMapped
data class CreateBookCommand(val author: String, val title: BookTitle, val numberOfPages: Int)

dataSource.transation {
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
