plugins {
   `java-gradle-plugin`
   id("com.gradle.plugin-publish") version "2.0.0"
   id("mikrom.conventions.lang.kotlin-jvm")
}

group = "io.github.kantis"
version = "0.1.0-SNAPSHOT"

dependencies {
   implementation(kotlin("gradle-plugin-api"))
}

gradlePlugin {
   website.set("https://github.com/kantis/mikrom")
   vcsUrl.set("https://github.com/kantis/mikrom.git")
   plugins {
      create("mikromGradlePlugin") {
         id = "io.github.kantis.mikrom"
         version = project.version.toString()
         displayName = "Mikrom Plugin"
         description = "Mikrom Gradle plugin for Kotlin. Generates JDBC/R2DBC row- and parameter-mappers."
         implementationClass = "io.github.kantis.mikrom.gradle.MikromGradlePlugin"
         tags.set(listOf("kotlin", "mikrom"))
      }
   }
}
