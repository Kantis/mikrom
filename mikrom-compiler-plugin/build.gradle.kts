import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

plugins {
   id("mikrom.conventions.lang.kotlin-jvm")
   id("mikrom.conventions.publishing.maven-publish")
   `java-test-fixtures`
}

group = "io.github.kantis"
version = "0.1.0-SNAPSHOT"

kotlin {
   compilerOptions {
      optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
      optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
   }
}

sourceSets {
   main {
      java.setSrcDirs(listOf("src"))
      resources.setSrcDirs(listOf("resources"))
   }

   testFixtures {
      java.setSrcDirs(listOf("testFixtures"))
   }

   test {
      java.setSrcDirs(listOf("test", "test-gen"))
      resources.setSrcDirs(listOf("testResources"))
   }
}

val mikromRuntimeClasspath: Configuration by configurations.creating { isTransitive = false }

dependencies {
   compileOnly(kotlin("compiler"))

   testFixturesApi(kotlin("test-junit5"))
   testFixturesApi(kotlin("compiler-internal-test-framework"))
   testFixturesApi(kotlin("compiler"))

   mikromRuntimeClasspath(projects.mikrom.mikromCore)

   testRuntimeOnly(kotlin("reflect"))
   testRuntimeOnly(kotlin("test"))
   testRuntimeOnly(kotlin("script-runtime"))
   testRuntimeOnly(kotlin("annotations-jvm"))
}

tasks.withType<Test>().configureEach {
   dependsOn(mikromRuntimeClasspath)
   inputs.dir(layout.projectDirectory.dir("testData"))
      .withPropertyName("testData")
      .withPathSensitivity(PathSensitivity.RELATIVE)

   workingDir = projectDir

   useJUnitPlatform()

   systemProperty("mikromRuntime.classpath", mikromRuntimeClasspath.asPath)

   // Properties required to run the internal test framework.
   systemProperty("idea.ignore.disabled.plugins", "true")
   systemProperty("idea.home.path", project.rootDir)
   setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
   setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
   setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
   setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
   setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
   setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")
}

val generateTests by tasks.registering(JavaExec::class) {
   classpath = sourceSets.testFixtures.get().runtimeClasspath
   mainClass.set("io.github.kantis.mikrom.plugin.GenerateTestsKt")
   workingDir = projectDir

   inputs.dir(layout.projectDirectory.dir("testData"))
      .withPropertyName("testData")
      .withPathSensitivity(PathSensitivity.RELATIVE)
   outputs.dir(layout.projectDirectory.dir("test-gen"))
      .withPropertyName("generatedTests")
}

// TODO: Only disable for the generated tests?
tasks.withType<KtLintCheckTask> {
   enabled = false
}

tasks.compileTestKotlin {
   dependsOn(generateTests)
}

fun Test.setLibraryProperty(
   propName: String,
   jarName: String,
) {
   val path = project.configurations
      .testRuntimeClasspath.get()
      .files
      .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
      ?.absolutePath
      ?: return
   systemProperty(propName, path)
}
