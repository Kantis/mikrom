## Project Overview

Mikrom is a Kotlin multiplatform micro ORM inspired by Dapper.
It focuses on explicit database operations without automatic change tracking or reflection-based mapping.
The project uses a compiler plugin to generate row mappers at compile time.

## Architecture

The project is structured as a multi-module Kotlin multiplatform project using Gradle composite builds:

- **mikrom-core**: Core abstractions including `Mikrom`, `DataSource`, `Transaction`, `RowMapper` and `ParameterMapper interfaces
- **mikrom-jdbc**: JDBC-specific implementation providing `JdbcDataSource` and result set mapping
- **mikrom-r2dbc**: R2DBC-specific implementation providing `PooledR2dbcDataSource` and result set mapping
- **mikrom-compiler-plugin**: Kotlin compiler plugin that generates `RowMapper` and `ParameterMapper` implementations at compile time
- **example**: Sample usage demonstrating the API
- **build-logic**: Custom Gradle build conventions and plugins

Key architectural principles:
- All database operations must occur within explicit transactions
- Row/Parameter mapping is compile-time generated, not reflection-based
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

When the compile plugin's IR output, the test's *.txt files will need to be regenerated.
Delete them and then run the tests (which will fail once) then run them again to ensure they are working.

## Core Components

### Transaction Management
- All database operations must be wrapped in transactions using `DataSource.transaction { }`. This is enforced at the API level - there are no methods that operate outside of a transaction context.
- All transactions must end by explicitly committing or rolling back.

### Row Mapping
The compiler plugin generates `RowMapper<T>` implementations for data classes annotated with `@MikromResult`. Mappers are registered with `Mikrom` instances and used to convert database rows to Kotlin objects.

### Parameter Mapping
Similarly, the compiler plugin generates `ParameterMapper<T>` for converting a Kotlin object into a map of String -> Any? (along with type
information), that is used to bind parameters to queries.

### Query Execution
- Use `mikrom.queryFor<T>("SELECT ... FROM ..", ...paramSets)` for SELECT operations. A paramSet is a list of Any? that is bound to the
  parameters in the query. Both positional or named parameter binding is supported
- Use `mikrom.execute("INSERT/UPDATE/DELETE ...", ...paramSets)` for INSERT/UPDATE/DELETE operations


## Testing
- The project uses Kotest for testing
- JDBC/R2DBC tests use Testcontainers and test factories from Kotest to create tests for each drivers
- Compiler plugin tests use the Kotlin compiler testing infrastructure
