plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}


group = "com.mxmariner.regatta"
version = "1.0"


kotlin {
    jvmToolchain(17)

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
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${serializationVersion}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}