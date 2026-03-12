plugins {
   id("mikrom.conventions.lang.kotlin-multiplatform-jvm")
   id("io.github.kantis.mikrom")
}

kotlin {
   sourceSets {
      jvmMain {
         dependencies {
            implementation(libs.mikrom.core)
            implementation(libs.mikrom.jdbc)
         }
      }

      jvmTest {
         dependencies {
            implementation(libs.kotest.assertionsCore)
            implementation(libs.kotest.frameworkEngine)
            implementation(libs.kotest.runnerJunit5)
            implementation(libs.testcontainers.postgresql)
            implementation(libs.postgresql)
            implementation(libs.hikari)
         }
      }
   }
}

tasks.named("compileKotlinJvm").configure {
   outputs.upToDateWhen { false }
}
