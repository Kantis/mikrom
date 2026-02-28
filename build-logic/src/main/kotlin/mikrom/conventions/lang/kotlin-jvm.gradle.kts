package mikrom.conventions.lang

import mikrom.conventions.MikromBuildLogicSettings
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

plugins {
   kotlin("jvm")
   id("mikrom.conventions.base")
   id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
   version.set("1.8.0")
}

val mikromSettings = extensions.getByType<MikromBuildLogicSettings>()

extensions.configure<KotlinJvmExtension> {
   explicitApi()

   jvmToolchain {
      languageVersion.set(JavaLanguageVersion.of(mikromSettings.jvmTarget.get()))
   }

   compilerOptions {
      optIn.add("kotlin.RequiresOptIn")
      optIn.add("io.github.kantis.mikrom.MikromInternal")
      freeCompilerArgs.add("-Xcontext-parameters")
   }

   sourceSets.configureEach {
      languageSettings {
         languageVersion = mikromSettings.kotlinTarget.get()
         apiVersion = mikromSettings.kotlinTarget.get()
      }
   }
}

tasks.withType<Test>().configureEach {
   useJUnitPlatform()
}
