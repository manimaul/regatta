import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.9.10"
    id("org.jetbrains.compose") version composeVersion
    id("io.ktor.plugin") version ktorVersion
}

group = "com.mxmariner.regatta"
version = "1.0"

repositories {
    google()
    mavenCentral()
}


kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js(IR) {
        browser {
            testTask(Action {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    useFirefox()
                }
            })
            //https://kotlinlang.org/docs/js-project-setup.html
            commonWebpackConfig(Action {
                output?.library = "AppComposables"
            })
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(compose.runtime)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core-jvm")
                implementation("io.ktor:ktor-server-netty-jvm")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("io.ktor:ktor-server-status-pages:${ktorVersion}")
                implementation(compose.runtime)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-tests-jvm")
            }
        }
    }
}

application {
    mainClass.set("com.mxmariner.regatta.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.named<Copy>("jvmProcessResources") {
    val jsDist = tasks.named("jsBrowserProductionWebpack")
    from(jsDist) {
        into("static")
    }
    exclude("webpack.config.js")
}

tasks.named<KotlinWebpack>("jsBrowserProductionWebpack") {
    doFirst {
        File("webpack.config.d/dev_server_config.js")
            .renameTo(File("webpack.config.d/dev_server_config"))
    }
    doLast {
        File("webpack.config.d/dev_server_config")
            .renameTo(File("webpack.config.d/dev_server_config.js"))
    }
}

afterEvaluate {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        nodeVersion = "16.0.0"
    }

    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().resolution("colors", "1.4.0")
    }
}
