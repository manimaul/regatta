
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}


group = "com.mxmariner.regatta"
version = "1.0"


kotlin {
    jvmToolchain(17)

    jvm {
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
//            commonWebpackConfig(Action {
//                output?.library = "AppComposables"
//            })
        }
        binaries.executable()
    }
    sourceSets {
//        val jsMain by getting {
//            dependencies {
////                implementation(compose.html.core)
////                implementation(compose.runtime)
////                implementation(compose.html.svg)
////                implementation(npm("sortablejs", "1.15.2"))
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
//            }
//        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
//            }
//        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
//        val jvmMain by getting {
//            dependencies {
////                implementation("io.ktor:ktor-server-core-jvm")
////                implementation("io.ktor:ktor-server-netty-jvm")
////                implementation("ch.qos.logback:logback-classic:$logbackVersion")
////                implementation("io.ktor:ktor-server-status-pages:${ktorVersion}")
////                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
//                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
////                implementation("io.ktor:ktor-server-auth:$ktorVersion")
////                implementation("io.ktor:ktor-server-compression:$ktorVersion")
////                compileOnly(compose.runtime)
//
////                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
////                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
////                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
////                implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
////                implementation("org.postgresql:postgresql:42.5.4")
//            }
//        }
//        val jvmTest by getting {
//            dependencies {
//                implementation("io.ktor:ktor-server-tests-jvm")
//            }
//        }
    }
}