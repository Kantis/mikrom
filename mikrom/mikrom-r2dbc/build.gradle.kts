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
               implementation(libs.h2)
               implementation(libs.r2dbc.h2)
               implementation(libs.r2dbc.postgresql)
               implementation(libs.r2dbc.mysql)
               implementation(libs.r2dbc.mariadb)
               implementation(libs.r2dbc.mssql)
               implementation(libs.r2dbc.oracle)
               implementation(libs.mysql.connector)
               implementation(libs.mariadb.connector)
               implementation(libs.mssql.connector)
               implementation(libs.oracle.connector)
               implementation(libs.logback.classic)
               implementation(libs.kotestExtensions.testcontainers)
               implementation(libs.testcontainers.postgresql)
               implementation(libs.testcontainers.mysql)
               implementation(libs.testcontainers.mariadb)
               implementation(libs.testcontainers.mssql)
               implementation(libs.testcontainers.oracle)
               implementation(libs.hikari)
            }
         }
      }
   }
}
