package io.github.kantis.mikrom.plugin

import io.github.kantis.mikrom.plugin.fir.MikromFirExtensionRegistrar
import io.github.kantis.mikrom.plugin.ir.MikromIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
public class MikromCompilerPluginRegistrar : CompilerPluginRegistrar() {
   override val pluginId: String = "io.github.kantis.mikrom.plugin"
   override val supportsK2: Boolean = true

   override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
      FirExtensionRegistrarAdapter.registerExtension(
         MikromFirExtensionRegistrar(),
      )
      IrGenerationExtension.registerExtension(
         MikromIrGenerationExtension(),
      )
   }
}
