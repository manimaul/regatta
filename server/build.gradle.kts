plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.ktor.plugin") version ktorVersion
}


group = "com.mxmariner.regatta"
version = "1.0"


kotlin {
    jvmToolchain(25)

    jvm {
        binaries {
            executable {
                mainClass.set("com.mxmariner.regatta.ApplicationKt")
                applicationName = "regatta"
            }
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation("io.ktor:ktor-server-core-jvm")
                implementation("io.ktor:ktor-server-netty-jvm")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("io.ktor:ktor-server-status-pages:${ktorVersion}")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-server-auth:$ktorVersion")
                implementation("io.ktor:ktor-server-compression:$ktorVersion")

                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-migration-jdbc:${exposedVersion}")
                implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
                implementation("org.postgresql:postgresql:42.5.4")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.testcontainers:testcontainers:$testcontainersVersion")
                implementation("org.testcontainers:postgresql:$testcontainersVersion")
            }
        }
    }
}

// Golden regression tests (docs/explore_implemented/test_audit_and_plan.md §5) need a container
// runtime, so they run in their own task and are excluded from the everyday jvmTest.
tasks.named<Test>("jvmTest") {
    filter.excludeTestsMatching("*GoldenTest")
}

// The podman machine's API socket, for Testcontainers on macOS. Evaluated lazily, only when needed.
val podmanSocket = providers.exec {
    commandLine("podman", "machine", "inspect", "--format", "{{.ConnectionInfo.PodmanSocket.Path}}")
    isIgnoreExitValue = true
}

tasks.register<Test>("jvmGoldenTest") {
    description = "Golden regression of race reports and standings against the production snapshot (needs podman or docker). " +
            "Pass -Dgolden.approve=true to re-approve the outputs."
    group = "verification"
    val jvmTest = tasks.named<Test>("jvmTest").get()
    dependsOn(jvmTest.dependsOn)
    testClassesDirs = jvmTest.testClassesDirs
    classpath = jvmTest.classpath
    filter.includeTestsMatching("*GoldenTest")
    outputs.upToDateWhen { false }
    testLogging.showStandardStreams = true
    providers.systemProperty("golden.approve").orNull?.let { systemProperty("golden.approve", it) }
    doFirst {
        val dockerSocket = File("/var/run/docker.sock")
        if (System.getenv("DOCKER_HOST") == null && !dockerSocket.exists()) {
            val path = runCatching { podmanSocket.standardOutput.asText.get().trim() }.getOrDefault("")
            val ok = runCatching { podmanSocket.result.get().exitValue == 0 }.getOrDefault(false)
            if (!ok || path.isEmpty()) {
                throw GradleException(
                    "jvmGoldenTest needs a container runtime: DOCKER_HOST is not set, there is no /var/run/docker.sock, " +
                            "and `podman machine inspect` failed. Start the podman machine (`podman machine start`) or set DOCKER_HOST."
                )
            }
            environment("DOCKER_HOST", "unix://$path")
        }
        if (System.getenv("TESTCONTAINERS_RYUK_DISABLED") == null) {
            environment("TESTCONTAINERS_RYUK_DISABLED", "true")
        }
    }
}
