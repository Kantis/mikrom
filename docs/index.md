# Mikrom

Mikrom is a Kotlin multiplatform micro ORM inspired by [Dapper](https://github.com/DapperLib/Dapper).

!!! warning "Experimental"
    This library is in the concept stage. The API is highly likely to change, and the compiler plugin is work in progress.

## Philosophy

- Multiplatform design, with JDBC support (R2DBC planned)
- Explicit transaction management
- Explicit SQL, instead of generated SQL which might not perform well
- Explicit updates, instead of automated tracking
- Convenient DSLs, so explicitness doesn't become tedious
- Code generation through compiler plugins, to support basic Row/Parameter mapping
- Minimal reflection

Mikrom is inspired by [Dapper](https://github.com/DapperLib/Dapper) and [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization).
