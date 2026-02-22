## Project Overview

Mikrom is a Kotlin multiplatform micro ORM inspired by Dapper.
It focuses on explicit database operations without automatic change tracking or reflection-based mapping.
The project uses a compiler plugin to generate row mappers at compile time.

## Architecture

The project is structured as a multi-module Kotlin multiplatform project using Gradle composite builds:

- **mikrom-core**: Core abstractions including `Mikrom`, `DataSource`, `Transaction`, and `RowMapper` interfaces
- **mikrom-jdbc**: JDBC-specific implementation providing `JdbcDataSource` and result set mapping
- **mikrom-compiler-plugin**: Kotlin compiler plugin that generates `RowMapper` implementations at compile time
- **example**: Sample usage demonstrating the API
- **build-logic**: Custom Gradle build conventions and plugins

Key architectural principles:
- All database operations must occur within explicit transactions
- Row mapping is compile-time generated, not reflection-based
- Command-query separation is encouraged
- Multiplatform design with platform-specific datasource implementations

## Development Commands

### Build and Test
- `./gradlew build` - Build all modules and run tests
- `./gradlew test` - Run tests for all modules
- `./gradlew jvmTest` - Run JVM-specific tests
- `./gradlew allTests` - Run tests for all targets with aggregated report

### Code Quality
- `./gradlew ktlintFormat` - Format code according to ktlint rules

### Compiler Plugin Development
- `./gradlew :mikrom-compiler-plugin:test` - Run compiler plugin tests
- Generated test files are in `mikrom-compiler-plugin/test-gen/`
- Test data is in `mikrom-compiler-plugin/testData/`

### Publishing
- `./gradlew publishToMavenLocal` - Publish to local Maven repository
- `./gradlew publish` - Publish to configured repositories


## Core Components

### Transaction Management
- All database operations must be wrapped in transactions using `DataSource.transaction { }`. This is enforced at the API level - there are no methods that operate outside of a transaction context.
- All transactions must end by explicitly committing or rolling back.

### Row Mapping
The compiler plugin generates `RowMapper<T>` implementations for data classes annotated with `@RowMapped`. Mappers are registered with `Mikrom` instances and used to convert database rows to Kotlin objects.

### Query Execution
- Use `mikrom.queryFor<T>(Query(...), ...params)` for SELECT operations
- Use `mikrom.execute(Query(...), ...paramSets)` for INSERT/UPDATE/DELETE operations

## Testing
- The project uses standard Kotlin test framework
- JDBC tests use H2 in-memory database via `H2Helpers.kt`, as well as Postgres in a Docker container using Testcontainers
- Compiler plugin tests use the Kotlin compiler testing infrastructure
