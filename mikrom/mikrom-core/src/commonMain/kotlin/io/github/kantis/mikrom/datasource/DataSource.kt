package io.github.kantis.mikrom.datasource

public interface DataSource {
   public fun <T> transaction(block: Transaction.() -> T): T
}
