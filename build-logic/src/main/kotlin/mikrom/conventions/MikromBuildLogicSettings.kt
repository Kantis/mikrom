package mikrom.conventions

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * Common settings for configuring mikrom's build logic.
 *
 * The settings need to be accessible during configuration, so they should come from Gradle
 * properties or environment variables.
 */
abstract class MikromBuildLogicSettings
   @Inject
   constructor(
      private val providers: ProviderFactory,
   ) {
      val kotlinTarget: Provider<String> = mikromSetting("kotlinTarget", "2.1")
      val jvmTarget: Provider<String> = mikromSetting("jvmTarget", "21")

      /** Controls whether Kotlin Multiplatform JVM is enabled */
      val enableKotlinJvm: Provider<Boolean> = mikromFlag("enableKotlinJvm", true)

      /** Controls whether Kotlin Multiplatform JS is enabled */
      // Disabling for now, seems to be some compilation issue in Mikrom-core when using Kotlin/JS
      val enableKotlinJs: Provider<Boolean> = mikromFlag("enableKotlinJs", false)

      /** Controls whether Kotlin Multiplatform Native is enabled */
      val enableKotlinNative: Provider<Boolean> = mikromFlag("enableKotlinNative", false)

      /**
       * Comma separated list of MavenPublication names that will have the publishing task enabled.
       * The provided names will be matched ignoring case, and by prefix, so `iOS` will match
       * `iosArm64`, `iosX64`, and `iosSimulatorArm64`.
       *
       * This is used to avoid duplicate publications, which can occur when a Kotlin Multiplatform
       * project is published in CI/CD on different host machines (Linux, Windows, and macOS).
       *
       * For example, by including `jvm` in the values when publishing on Linux, but omitting `jvm` on
       * Windows and macOS, this results in any Kotlin/JVM publications only being published once.
       */
      val enabledPublicationNamePrefixes: Provider<Set<String>> =
         mikromSetting("enabledPublicationNamePrefixes", "KotlinMultiplatform,Jvm,Js,iOS,macOS,watchOS,tvOS,mingw")
            .map { enabledPlatforms ->
               enabledPlatforms
                  .split(",")
                  .map { it.trim() }
                  .filter { it.isNotBlank() }
                  .toSet()
            }

      private fun mikromSetting(
         name: String,
         default: String? = null,
      ) = providers.gradleProperty("mikrom_$name")
         .orElse(providers.provider { default }) // workaround for https://github.com/gradle/gradle/issues/12388

      private fun mikromFlag(
         name: String,
         default: Boolean,
      ) = providers.gradleProperty("mikrom_$name").map { it.toBoolean() }.orElse(default)

      companion object {
         const val EXTENSION_NAME = "mikromSettings"

         /**
          * Regex for matching the release version.
          *
          * If a version does not match this code it should be treated as a SNAPSHOT version.
          */
         val releaseVersionRegex = Regex("\\d+.\\d+.\\d+")
      }
   }
