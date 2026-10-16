plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

group = "com.mxmariner.regatta"
version = "1.0"

kotlin {
    js {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
                scssSupport { enabled.set(true) }
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

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":regattaCommon"))
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation(compose.html.svg)
                implementation(npm("maplibre-gl", "4.7.1"))
                implementation(npm("sortablejs", "1.15.2"))
                implementation(devNpm("tailwindcss", "4.3.3"))
                implementation(devNpm("@tailwindcss/postcss", "4.3.3"))
                implementation(devNpm("@tailwindcss/forms", "0.5.11"))
                implementation(devNpm("postcss", "8.5.30"))
                implementation(devNpm("postcss-loader", "8.2.1"))
                implementation(devNpm("compression-webpack-plugin", "11.1.0"))
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(compose.html.testUtils)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
