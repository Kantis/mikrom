plugins {
   id("mikrom.conventions.lang.kotlin-multiplatform-jvm")
   id("mikrom.conventions.publishing.maven-publish")
}

kotlin {
   sourceSets {
      commonTest {
         dependencies {
            implementation(libs.kotest.frameworkEngine)
            implementation(libs.kotest.assertionsCore)
            implementation(libs.kotest.assertionsJson)
            implementation(libs.kotest.property)
         }
      }

      if (mikromSettings.enableKotlinJvm.get()) {
         jvmMain {
            dependencies {
               api(projects.mikrom.mikromCore)
               api(libs.r2dbc.pool)
               // Probably not needed.. keeping it around for now
//               implementation(libs.kotlinxCoroutinesReactor)
               implementation(libs.kotlinxCoroutinesReactive)
               implementation(kotlin("reflect"))
               implementation(libs.slf4j.api)
            }
         }

         jvmTest {
            dependencies {
               implementation(libs.kotest.runnerJunit5)
//               implementation(libs.h2)
               implementation(libs.r2dbc.postgresql)
               implementation(libs.logback.classic)
               implementation(libs.kotestExtensions.testcontainers)
               implementation(libs.testcontainers.postgresql)
               implementation(libs.hikari)
            }
         }
      }
   }
}
