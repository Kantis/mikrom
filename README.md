![Stability Status - Alpha](https://kotl.in/badges/alpha.svg)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/kantis/ks3/ci.yaml?branch=main)

## What is Mikrom?
Mikrom is inspired by [Dapper](https://github.com/DapperLib/Dapper), a popular micro ORM for .NET.

## Usage
> Note: This library is in an early phase. The API is highly likely to have breaking changes sometimes, but I
> would value your feedback on the library design.

### JDBC
Add the Gradle plugin and dependency:

```kotlin
plugins {
  id("com.github.kantis.mikrom") version "0.5.0"
}

dependencies {
  implementation("io.github.kantis.mikrom:mikrom-jdbc:0.5.0")
}
```

Create a JDBC connection and construct a `Mikrom` instance:

```kotlin
fun main() {
  val dbConnection = DriverManager.getConnection("jdbc:your_database_url")
  val dataSource = JdbcDataSource(dbConnection)
  val mikrom = Mikrom {}

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

    mikrom.queryFor<Book>(
      "SELECT * FROM books WHERE number_of_pages > ?", 311
    ) // Book("George Orwell", BookTitle("1984"), 328)
  }
}

@MikromParameter
data class CreateBookCommand(val author: String, val title: BookTitle, val numberOfPages: Int)

@JvmInline
value class BookTitle(val title: String)

@MikromResult
data class Book(val author: String, val title: BookTitle, val numberOfPages: Int)
```

## Philosophy
- Multiplatform design, with JDBC support (R2DBC planned)
  - JS and Native variants not currently planned, but possible. Raise an issue if you want them, also include which underlying driver would make sense to use for that platform.
- Explicit transaction management
- Explicit SQL, instead of generated SQL which might not perform well
- Explicit updates, instead of automated tracking
- Convenient DSLs, so explicitness doesn't become tedious
- Code generation through compiler plugins, to support basic Row/Parameter mapping
- Minimal reflection

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
