# Mikrom

Mikrom is a Kotlin multiplatform micro ORM inspired by [Dapper](https://github.com/DapperLib/Dapper).

!!! warning "Alpha"
    This library is in an early stage. There will likely be breaking changes as the API evolves. Be prepared to put some effort into
    migrating your code when updating to new versions.

## Philosophy

- Multiplatform design, with JDBC and R2DBC support
- Explicit transaction management
- Explicit SQL, instead of generated SQL which might not perform well
- Explicit updates, instead of automated tracking
- Convenient DSLs, so explicitness doesn't become tedious
- Code generation through compiler plugins, to support basic Row/Parameter mapping
- Minimal reflection

Mikrom is inspired by [Dapper](https://github.com/DapperLib/Dapper) and [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization).
