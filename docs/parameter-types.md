# Parameter Types

## Nullable Parameters / TypedNull

When using `@MikromParameter` data classes with nullable properties, the compiler plugin automatically wraps null values in `TypedNull` to preserve type information during binding. This is especially important for R2DBC drivers that require explicit type information when binding null values.

```kotlin
@MikromParameter
data class User(
    val name: String,
    val nickname: String?,  // null values are automatically wrapped in TypedNull(String::class)
)
```

For raw parameter lists, you can use `TypedNull` directly:

```kotlin
mikrom.execute(
    "INSERT INTO users (name, nickname) VALUES (?, ?)",
    listOf("Alice", TypedNull(String::class)),
)
```

## VARCHAR vs NVARCHAR

### The Problem

When a Kotlin `String` is bound via JDBC's `setString()`, SQL Server (and some other databases) treat it as `NVARCHAR`. If the backing column is `VARCHAR`, the database performs an implicit conversion that **prevents index usage** and causes full table scans — even on indexed columns.

### Solution: `AnsiString` Wrapper

Wrap string values in `AnsiString` to force `VARCHAR` binding:

```kotlin
mikrom.execute(
    "SELECT * FROM users WHERE login = ?",
    listOf(AnsiString("alice")),
)
```

This causes the JDBC binding layer to use `setObject(index, value, Types.VARCHAR)` instead of `setString()`, avoiding the implicit `NVARCHAR` conversion.

### Solution: `@SqlTypeHint` Annotation

For `@MikromParameter` data classes, annotate constructor parameters with `@SqlTypeHint`:

```kotlin
@MikromParameter
data class UserQuery(
    @SqlTypeHint(SqlType.VARCHAR)
    val login: String,
    val active: Boolean,
)
```

The compiler plugin will automatically wrap the `login` value in `AnsiString` in the generated parameter mapper. Nullable properties are also supported — null values still use `TypedNull`, while non-null values are wrapped in `AnsiString`.

```kotlin
@MikromParameter
data class UserSearch(
    @SqlTypeHint(SqlType.VARCHAR)
    val login: String?,  // null → TypedNull(String::class), non-null → AnsiString(value)
)
```
