import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.mxmariner.regatta"
version = "1.0"

/*
   VersionInfo.kt: the git state and build time the About page shows. Generated into the build
   directory on every build (the date alone makes it never up to date) and added to commonMain,
   never written into src/. The git commands run in this directory, so inside parent project they
   describe the regatta submodule's checkout; a detached HEAD gives an empty branch.
*/
fun git(vararg args: String) = providers.exec {
    workingDir = projectDir
    commandLine("git", *args)
    isIgnoreExitValue = true
}

val generateVersionInfo by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/versionInfo")
    val gitHash = git("rev-parse", "--verify", "--short", "HEAD").standardOutput.asText.map { it.trim() }
    val gitBranch = git("symbolic-ref", "--short", "-q", "HEAD").standardOutput.asText.map { it.trim() }
    val dirty = git("diff-index", "--quiet", "HEAD", "--").result.map { it.exitValue != 0 }
    val version = project.version.toString()
    inputs.property("gitHash", gitHash)
    inputs.property("gitBranch", gitBranch)
    inputs.property("dirty", dirty)
    inputs.property("version", version)
    outputs.dir(outDir)
    outputs.upToDateWhen { false }
    doLast {
        val date = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(Date())
        outDir.get().file("VersionInfo.kt").asFile.writeText(
            """
            object VersionInfo {
                const val gitHash = "${gitHash.get()}"
                const val gitBranch = "${gitBranch.get()}"
                const val version = "$version"
                const val dev = "${dirty.get()}"
                const val buildDate = "$date"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    jvmToolchain(21)

    jvm { }
    js(IR) {
        browser {
            testTask(Action {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    useFirefox()
                }
            })
        }
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generateVersionInfo)
            dependencies {
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
