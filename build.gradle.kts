import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.lang.IllegalStateException

plugins {
    kotlin("multiplatform") version kotlinVersion
    id("org.jetbrains.compose") version composeVersion
    id("io.ktor.plugin") version ktorVersion
    kotlin("plugin.serialization") version kotlinVersion
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
                implementation(compose.html.svg)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
            }
        }
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
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-server-auth:$ktorVersion")
                compileOnly(compose.runtime)

                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
                implementation("org.postgresql:postgresql:42.5.4")
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

afterEvaluate {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        nodeVersion = "16.0.0"
    }

    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().resolution("colors", "1.4.0")
    }
}

tasks.named("installDist") {
    dependsOn("jsBrowserProductionWebpack")
}

task<Exec>("makeImg") {
    dependsOn( "installDist")
    commandLine("bash", "-c", "docker build -t ghcr.io/manimaul/regatta:latest .")
}


task<Exec>("pubImg") {
    mustRunAfter(":makeImg")
    commandLine("bash", "-c", "docker push ghcr.io/manimaul/regatta:latest")
}

task<Exec>("k8sApplyServer") {
    mustRunAfter(":pubImg")
    commandLine("bash", "-c", "echo '${serverYaml()}' | kubectl apply -f -")
}

task<Exec>("k8sDeleteServer") {
    commandLine("bash", "-c", "echo '${serverYaml()}' | kubectl delete -f -")
}

task<Exec>("k8sApplyDatabase") {
    commandLine("bash", "-c", "echo '${dbYaml()}' | kubectl apply -f -")
}

task<Exec>("k8sDeleteDatabase") {
    mustRunAfter(":pubImg")
    commandLine("bash", "-c", "echo '${dbYaml()}' | kubectl delete -f -")
}

task<Exec>("holdOn") {
    mustRunAfter(":pubImg")
    commandLine("bash", "-c", "echo 'hold on' && sleep 5")
}

task<Exec>("cyclePods") {
    mustRunAfter(":k8sApplyServer", ":pubImg", ":holdOn")
    commandLine("bash", "-c", "kubectl -n regatta delete pods -l app=regatta-service")
}

tasks.register<GradleBuild>("deployServer") {
    tasks = listOf(":makeImg", ":pubImg", ":k8sApplyServer", ":holdOn", ":cyclePods")
}

fun getProperty(name: String) : String {
    if (project.hasProperty(name)) {
        return "${project.property(name)}"
    } else {
        throw IllegalStateException("$name missing from ~/.gradle/gradle.properties")
    }
}

fun dbYaml() : String {
    val pgUser = getProperty("REGATTA_PG_USER")
    val pgPass = getProperty("REGATTA_PG_PASS")
    val root = File(rootProject.projectDir, "k8s_deploy/database.yaml")
    return deploymentYaml(root, pgUser, pgPass)
}

fun serverYaml() : String {
    val pgUser = getProperty("REGATTA_PG_USER")
    val pgPass = getProperty("REGATTA_PG_PASS")
    val root = File(rootProject.projectDir, "k8s_deploy/server.yaml")
    return deploymentYaml(root, pgUser, pgPass)
}

fun deploymentYaml(root: File, pgUser: String, pgPass: String): String {
    return root .inputStream()
        .readBytes()
        .toString(Charsets.UTF_8)
        .replace(
            "{REGATTA_PG_USER}",
            "'$pgUser'"
        )
        .replace(
            "{REGATTA_PG_PASS}",
            "'$pgPass'"
        )
}
