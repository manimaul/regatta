plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
}

group = "com.mxmariner.regatta"
version = "0.0.1"

application {
    mainClass.set("com.mxmariner.regatta.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-status-pages:${ktorVersion}")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

tasks {
    named<JavaExec>("run") {
        //./gradlew :chart_server:run -Pskip
        if (project.hasProperty("skip")) {
            println("skipping web build")
        } else {
            dependsOn(":frontend:jsBrowserProductionWebpack")
        }
    }
}

