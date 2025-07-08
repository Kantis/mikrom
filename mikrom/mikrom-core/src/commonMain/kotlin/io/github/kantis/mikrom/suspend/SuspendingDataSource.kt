package io.github.kantis.mikrom.suspend

public interface SuspendingDataSource {
   public suspend fun <T> suspendingTransaction(block: suspend SuspendingTransaction.() -> T): T
}
