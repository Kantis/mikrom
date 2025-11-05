![Stability Status - Experimental](https://kotl.in/badges/experimental.svg)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/kantis/ks3/ci.yaml?branch=main)

## What is Mikrom?
Mikrom is inspired by [Dapper](https://github.com/DapperLib/Dapper), a popular micro ORM for .NET.

> This library is in the concept stage. The API is highly likely to change, and the compiler plugin is work in progress.
> Only get involved at this point if you are interested in helping shape the design.

## Philosophy
- No automated tracking of changes
- No reflection-based mapping
  - Note that reflection might be necessary on _some_ platforms, however. See for instance [KxS](https://github.com/search?q=repo:Kotlin/kotlinx.serialization%20findAssociatedObject&type=code) which uses reflection to find serializers on K/N and K/JS.
- Provide convenient DSLs, so the explicitness doesn't become tedious
- Explicit transaction management is mandatory
- Multiplatform design, with JDBC and R2DBC (to-be-added) support added by separate modules.
  - JS and Native variants not currently planned, raise an issue if you want them.

I want to provide a simple way of working with databases, that does _not_ involve automatic change tracking in "entities".
I want explicit control over what is being updated, and how. With Mikrom, you are in control and can write SQL that fits your exact needs.

Command-query separation becomes natural, since there is no relationship between reading and writing data tied to some "entity" class.

## Usage
> Note: This library is in the concept stage. The API is highly likely to change, and the compiler plugin is work in progress.

### JDBC
Create a JDBC connection and construct a `Mikrom` instance:

```kotlin
val dbConnection = DriverManager.getConnection("jdbc:your_database_url")
val jdbcDataSource = JdbcDataSource(dbConnection)

data class Book(val author: String, val title: String, val numberOfPages: Int)

val mikrom = Mikrom {
  registerMapper { row ->
    Book(
      author = row["author"] as String,
      title = row["title"] as String,
      numberOfPages = row["number_of_pages"] as Int
    )
  }
}

// Compilation error, `execute` is only defined in the context of a transaction.
mikrom.execute(
  Query("INSERT INTO books (author, title, number_of_pages) VALUES (?, ?, ?)"),
  listOf("JRR Tolkien", "The Hobbit", 310),
  listOf("George Orwell", "1984", 328),
)

dataSource.transaction {
  mikrom.execute(
    Query("INSERT INTO books (author, title, number_of_pages) VALUES (?, ?, ?)"),
    listOf("JRR Tolkien", "The Hobbit", 310),
    listOf("George Orwell", "1984", 328),
  )

  mikrom.queryFor<Book>(Query("SELECT * FROM books")) shouldBe
    listOf(
      Book("JRR Tolkien", "The Hobbit", 310),
      Book("George Orwell", "1984", 328),
    )

  mikrom.queryFor<Book>(Query("SELECT * FROM books WHERE number_of_pages > ?"), 320) shouldBe
    listOf(Book("George Orwell", "1984", 328))

  // The transaction scope must result in either Commit or Rollback.
  // If the block exits by throwing an exception, it will automatically roll back.
  TransactionResult.Commit
}
```

### Parameter mappers
Building on our previous example, lets say we want structured Query types. We can leverage ParameterMappers to achieve this.

```kotlin
data class SearchBooksByNumberOfPages(val minPages: Int)

val mikrom = Mikrom {
  registerParameterMapper<SearchBooksByNumberOfPages> { request ->
    mapOf("minPages" to request.minPages)
  }
}

dataSource.transaction {
  mikrom.queryFor<Book>(
    Query("SELECT * FROM books WHERE number_of_pages > :minPages"),
    SearchBooksbyNumberOfPages(320),
  ) shouldBe listOf(Book("George Orwell", "1984", 328))
}
```

### Compiler plugin
Using the compiler plugin, you can annotate your data classes with `@RowMapped` to automatically generate row mappers.

```kotlin
// This will automatically generate a `RowMapper<Book>` implementation and register it with Mikrom.
@RowMapped
data class Book(val author: String, val title: String, val numberOfPages: Int)
```


License
-------

    Copyright (C) 2025 Emil Kantis

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Code from other parties
- A ton of code, including entire infrastructure setup, related to compiler plugins has been copied from Brian Norman's [Buildable](https://github.com/bnorm/buildable) project
- Some code related to compiler plugins has been copied from Zac Sweers' [Metro](https://github.com/ZacSweers/metro) project
