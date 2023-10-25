rootProject.name = "regatta"
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin_version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}
include("backend")
include("frontend")
