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
               implementation(kotlin("reflect"))
               implementation(libs.slf4j.api)
            }
         }

         jvmTest {
            dependencies {
               implementation(libs.kotest.runnerJunit5)
               implementation(libs.h2)
               implementation(libs.postgresql)
               implementation(libs.logback.classic)
               implementation(libs.kotestExtensions.testcontainers)
               implementation(libs.testcontainers.postgresql)
               implementation(libs.hikari)
               implementation(libs.mysql.connector)
               implementation(libs.mariadb.connector)
               implementation(libs.mssql.connector)
               implementation(libs.oracle.connector)
               implementation(libs.testcontainers.mysql)
               implementation(libs.testcontainers.mariadb)
               implementation(libs.testcontainers.mssql)
               implementation(libs.testcontainers.oracle)
               implementation(libs.sqlite)
            }
         }
      }
   }
}
