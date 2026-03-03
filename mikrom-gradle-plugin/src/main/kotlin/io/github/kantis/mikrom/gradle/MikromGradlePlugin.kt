package io.github.kantis.mikrom.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class MikromGradlePlugin : KotlinCompilerPluginSupportPlugin {
   private lateinit var extension: MikromGradleExtension

   override fun apply(target: Project): Unit =
      with(target) {
         extension = extensions.create("mikrom", MikromGradleExtension::class.java)
      }

   override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

   override fun getCompilerPluginId(): String = "mikrom"

   override fun getPluginArtifact(): SubpluginArtifact =
      SubpluginArtifact(
         groupId = "io.github.kantis",
         artifactId = "mikrom-compiler-plugin",
         version = extension.compilerPluginVersion.get(),
      )

   override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
      val project = kotlinCompilation.target.project
      return project.provider {
         listOf()
      }
   }
}
