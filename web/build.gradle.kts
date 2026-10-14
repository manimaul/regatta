plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose") version kotlinVersion
    id("org.jetbrains.compose") version composeVersion
}

kotlin {

    js {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) } // Add this
                scssSupport { enabled.set(true) } // Add this
                outputFileName = "web.js"
            }
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    useFirefox()
                }
            }
        }
        binaries.executable()
    }
    repositories {
        mavenLocal()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation("org.jetbrains.compose.html:html-core:$composeVersion")
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.html:html-svg:$composeVersion")
                implementation(npm("maplibre-gl", "4.7.1"))
                implementation(npm("sortablejs", "1.15.2"))
                implementation(devNpm("tailwindcss", "4.3.3"))
                implementation(devNpm("@tailwindcss/postcss", "4.3.3"))
                implementation(devNpm("@tailwindcss/forms", "0.5.11"))
                implementation(devNpm("postcss", "8.5.30"))
                implementation(devNpm("postcss-loader", "8.2.1"))
                implementation(devNpm("compression-webpack-plugin", "11.1.0"))
                implementation("io.madrona:geojson:1.3-SNAPSHOT")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("org.jetbrains.compose.html:html-test-utils:$composeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
            }
        }
    }
}

