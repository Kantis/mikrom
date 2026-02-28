package mikrom.conventions.lang

import mikrom.conventions.MikromBuildLogicSettings
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun

plugins {
   kotlin("multiplatform")
   id("mikrom.conventions.base")
   id("io.kotest")
   id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
   version.set("1.8.0")
}

// Base configuration for all Kotlin/Multiplatform conventions.
// This plugin does not enable any Kotlin target. To enable a target in a subproject, prefer
// applying specific Kotlin target convention plugins.

val mikromSettings = extensions.getByType<MikromBuildLogicSettings>()

kotlin {
   explicitApi()
   jvmToolchain {
      languageVersion.set(JavaLanguageVersion.of(mikromSettings.jvmTarget.get()))
   }

   sourceSets {
      all {
         languageSettings.optIn("io.github.kantis.mikrom.MikromInternal")
         languageSettings.optIn("kotlin.RequiresOptIn")
      }
   }

   compilerOptions {
      freeCompilerArgs.add("-Xcontext-parameters")
   }

   // configure all Kotlin/JVM Tests to use JUnit
   targets.withType<KotlinJvmTarget>().configureEach {
      testRuns.configureEach {
         executionTask.configure {
            useJUnitPlatform()
         }
      }
   }

   sourceSets.configureEach {
      languageSettings {
         languageVersion = mikromSettings.kotlinTarget.get()
         apiVersion = mikromSettings.kotlinTarget.get()
         optIn("kotlin.RequiresOptIn")
      }
   }
}

fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else this }

// create lifecycle task for each Kotlin Platform, that will run all tests
KotlinPlatformType.values().forEach { kotlinPlatform ->
   val kotlinPlatformName = kotlinPlatform.name.capitalize()

   val testKotlinTargetLifecycleTask =
      tasks.create("allKotlin${kotlinPlatformName}Tests") {
         group = LifecycleBasePlugin.VERIFICATION_GROUP
         description = "Run all Kotlin/$kotlinPlatformName tests"
      }

   kotlin.testableTargets.matching {
      it.platformType == kotlinPlatform
   }.configureEach {
      testRuns.configureEach {
         if (this is KotlinTaskTestRun<*, *>) {
            testKotlinTargetLifecycleTask.dependsOn(executionTask)
         }
      }
   }
}
