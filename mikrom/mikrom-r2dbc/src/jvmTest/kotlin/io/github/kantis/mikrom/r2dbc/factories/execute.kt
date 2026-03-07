package io.github.kantis.mikrom.r2dbc.factories

import io.github.kantis.mikrom.Mikrom
import io.github.kantis.mikrom.suspend.SuspendingTransaction
import io.github.kantis.mikrom.suspend.execute
import io.github.kantis.mikrom.suspend.executeStreaming
import kotlinx.coroutines.flow.flowOf

context(transaction: SuspendingTransaction)
suspend fun Mikrom.testExecute(
   query: String,
   streaming: Boolean,
   vararg parameters: List<Any>,
) {
   if (streaming) {
      executeStreaming(query, flowOf(*parameters)).join()
   } else {
      execute(query, *parameters)
   }
}
