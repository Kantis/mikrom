package mikrom.conventions.publishing

import mikrom.conventions.MikromBuildLogicSettings

plugins {
   id("com.gradleup.nmcp")
   signing
   `maven-publish`
}

val mikromSettings = extensions.getByType<MikromBuildLogicSettings>()

val javadocJarStub by tasks.registering(Jar::class) {
   group = JavaBasePlugin.DOCUMENTATION_GROUP
   description = "Empty Javadoc Jar (required by Maven Central)"
   archiveClassifier.set("javadoc")
}

publishing {
   repositories {
      // Publish to a project-local Maven directory, for verification. To test, run:
      // ./gradlew publishAllPublicationsToMavenProjectLocalRepository
      // and check $rootDir/build/maven-project-local
      maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
         name = "MavenProjectLocal"
      }
   }

   publications.withType<MavenPublication>().forEach {
      it.apply {

         artifact(javadocJarStub)

         pom {
            name.set("mikrom")
            description.set("KotlinX Serialization standard serializers")
            url.set("https://github.com/Kantis/mikrom")

            scm {
               connection.set("scm:git:https://github.com/Kantis/mikrom/")
               developerConnection.set("scm:git:https://github.com/Kantis/")
               url.set("https://github.com/Kantis/mikrom")
            }

            licenses {
               license {
                  name.set("Apache-2.0")
                  url.set("https://opensource.org/licenses/Apache-2.0")
               }
            }

            developers {
               developer {
                  id.set("Kantis")
                  name.set("Emil Kantis")
                  email.set("emil.kantis@protonmail.com")
               }
            }
         }
      }
   }
}

signing {
   val signingKey: String? by project
   val signingPassword: String? by project

   logger.lifecycle("[maven-publish convention] signing is enabled for ${project.path}")
   if (signingKey.isNullOrBlank() || signingPassword.isNullOrBlank()) {
      logger.lifecycle("[maven-publish convention] signing key or password is not set, skipping signing")
      return@signing
   }
   useGpgCmd()
   useInMemoryPgpKeys(signingKey, signingPassword)
   sign(publishing.publications)
}

// https://youtrack.jetbrains.com/issue/KT-46466
val signingTasks = tasks.withType<Sign>()

tasks.withType<AbstractPublishToMaven>().configureEach {
   // Gradle warns about some signing tasks using publishing task outputs without explicit dependencies.
   // Here's a quick fix.
   dependsOn(signingTasks)
   mustRunAfter(signingTasks)

   // use a val for the GAV to avoid Gradle Configuration Cache issues
   val publicationGAV = publication?.run { "io.github.kantis:$artifactId:$version" }

   doLast {
      if (publicationGAV != null) {
         logger.lifecycle("[task: $path] $publicationGAV")
      }
   }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
   // use vals - improves Gradle Config Cache compatibility
   val publicationName = publication.name
   val enabledPublicationNamePrefixes = mikromSettings.enabledPublicationNamePrefixes

   val isPublicationEnabled = enabledPublicationNamePrefixes.map { prefixes ->
      prefixes.any { prefix -> publicationName.startsWith(prefix, ignoreCase = true) }
   }

   // register an input so Gradle can do up-to-date checks
   inputs.property("publicationEnabled", isPublicationEnabled)

   onlyIf {
      val enabled = isPublicationEnabled.get()
      if (!enabled) {
         logger.lifecycle("[task: $path] publishing for $publicationName is disabled")
      }
      enabled
   }
}

// Kotlin Multiplatform specific publishing configuration
// plugins.withType<KotlinMultiplatformPluginWrapper>().configureEach {
//   // nothing yet!
// }
