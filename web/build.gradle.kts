plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose") version kotlinVersion
    id("org.jetbrains.compose") version composeVersion
}

kotlin {

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
        val jsMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation(compose.html.svg)
                implementation(npm("sortablejs", "1.15.2"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

tasks.named("jsBrowserTest") {
    doFirst {
        File("webpack.config.d/dev_server_config.js")
            .renameTo(File("webpack.config.d/dev_server_config"))
    }
    doLast {
        File("webpack.config.d/dev_server_config")
            .renameTo(File("webpack.config.d/dev_server_config.js"))
    }
}

