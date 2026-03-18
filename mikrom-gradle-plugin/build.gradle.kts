plugins {
   `java-gradle-plugin`
   id("com.gradle.plugin-publish") version "2.0.0"
   id("mikrom.conventions.lang.kotlin-jvm")
}

group = "io.github.kantis.mikrom"
version = project.property("version") ?: "0.2.0-SNAPSHOT"

dependencies {
   implementation(kotlin("gradle-plugin-api"))
}

val generateVersionProperties by tasks.registering {
   val outputDir = layout.buildDirectory.dir("generated/mikrom-resources")
   val pluginVersion = project.version.toString()
   inputs.property("version", pluginVersion)
   outputs.dir(outputDir)
   doLast {
      val propsFile = outputDir.get().file("mikrom-gradle-plugin.properties").asFile
      propsFile.parentFile.mkdirs()
      propsFile.writeText("version=$pluginVersion\n")
   }
}

sourceSets.main {
   resources.srcDir(generateVersionProperties.map { it.outputs.files.singleFile })
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
