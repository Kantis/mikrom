plugins {
   id("mikrom.conventions.base")
   id("mikrom.conventions.api-validation")
   idea
}

group = "io.github.kantis"
version = "0.1.0-SNAPSHOT"

idea {
   module {
      isDownloadSources = true
      isDownloadJavadoc = false
      excludeDirs = excludeDirs +
         layout.files(
            ".idea",
            // location of the lock file, overridden by Kotlin/JS convention
            "gradle/kotlin-js-store",
            "gradle/wrapper",
         )

      // exclude generated Gradle code, so it doesn't clog up search results
      excludeDirs.addAll(
         layout.files(
            "build-logic/build/generated-sources/kotlin-dsl-accessors/kotlin/gradle",
            "build-logic/build/generated-sources/kotlin-dsl-external-plugin-spec-builders/kotlin/gradle",
            "build-logic/build/generated-sources/kotlin-dsl-plugins/kotlin",
            "build-logic/build/pluginUnderTestMetadata",
         ),
      )
   }
}


//tasks {
//   val apiCheck by registering {
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-core:apiCheck"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-jdbc:apiCheck"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-r2dbc:apiCheck"))
//      dependsOn(gradle.includedBuild("mikrom-compiler-plugin").task(":apiCheck"))
//      dependsOn(gradle.includedBuild("mikrom-gradle-plugin").task(":apiCheck"))
//   }
//
//   register("publishToMavenLocal").configure {
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-core:publishToMavenLocal"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-jdbc:publishToMavenLocal"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-r2dbc:publishToMavenLocal"))
//      dependsOn(gradle.includedBuild("mikrom-compiler-plugin").task(":publishToMavenLocal"))
//      dependsOn(gradle.includedBuild("mikrom-gradle-plugin").task(":publishPluginMavenPublicationToMavenLocal"))
//   }
//
//   named("check").configure {
//      dependsOn(gradle.includedBuilds.map { it.task(":check") })
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-core:check"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-jdbc:check"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-r2dbc:check"))
//      dependsOn(gradle.includedBuild("example").task(":jvmTest"))
//      dependsOn(apiCheck)
//   }
//
//   register("apiDump").configure {
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-core:apiDump"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-jdbc:apiDump"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-r2dbc:apiDump"))
//      dependsOn(gradle.includedBuild("mikrom-compiler-plugin").task(":apiDump"))
//      dependsOn(gradle.includedBuild("mikrom-gradle-plugin").task(":apiDump"))
//   }
//
//   register("ktlintFormat").configure {
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-core:ktlintFormat"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-jdbc:ktlintFormat"))
//      dependsOn(gradle.includedBuild("mikrom").task(":mikrom-r2dbc:ktlintFormat"))
//      dependsOn(gradle.includedBuild("mikrom-compiler-plugin").task(":ktlintFormat"))
//      dependsOn(gradle.includedBuild("mikrom-gradle-plugin").task(":ktlintFormat"))
//   }
//
//}
