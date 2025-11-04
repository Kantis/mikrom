rootProject.name = "mikrom-root"

includeBuild("build-logic")
include(
   ":mikrom:mikrom-core",
   ":mikrom:mikrom-jdbc",
   ":mikrom:mikrom-r2dbc",
   ":mikrom-compiler-plugin",
   ":mikrom-gradle-plugin",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

apply(from = "./build-logic/repositories.gradle.kts")
