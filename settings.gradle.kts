/*
   Regatta's own build, for working on it alone. It is also a submodule, whose settings include these same three projects, under the
   same paths, as subprojects of that build. Everything a project build file needs comes from a
   version catalog named `libs`, which each root supplies: gradle/libs.versions.toml here, and
   parent at its root. The project paths carry the regatta prefix so they are the same from
   both roots and collide with nothing in a parent repo.
*/
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "regatta"

fun includeAt(path: String, dir: String) {
    include(path)
    project(path).projectDir = file(dir)
}

includeAt(":regattaCommon", "common")
includeAt(":regattaServer", "server")
includeAt(":regattaWeb", "web")
