package io.github.kantis.mikrom.plugin

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsTest
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractDiagnosticTest : AbstractFirLightTreeDiagnosticsTest() {
   override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider =
      ClasspathBasedStandardLibrariesPathProvider

   override fun configure(builder: TestConfigurationBuilder) {
      super.configure(builder)

      with(builder) {
         configurePlugin()
      }
   }
}
