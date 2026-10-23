plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.mxmariner.regatta"
version = "1.0"

kotlin {
    jvmToolchain(21)

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
                implementation(project(":regattaCommon"))
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.logback.classic)
                implementation(libs.ktor.server.status.pages)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.auth)
                implementation(libs.ktor.server.compression)

                implementation(libs.exposed.core)
                implementation(libs.exposed.dao)
                implementation(libs.exposed.jdbc)
                implementation(libs.exposed.json)
                implementation(libs.exposed.migration.jdbc)
                implementation(libs.exposed.kotlin.datetime)
                implementation(libs.postgresql)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktor.server.test.host)
                implementation(libs.testcontainers.core)
                implementation(libs.testcontainers.postgresql)
            }
        }
    }
}

/*
   The web app's production bundle is served from the server's resources under static/, beside
   common's static files (index.html and the images). jsBrowserProductionWebpack's output is the
   bundle alone; webpack.config.js is its own input, not something to ship.
*/
evaluationDependsOn(":regattaWeb")
tasks.named<Copy>("jvmProcessResources") {
    from(project(":regattaWeb").tasks.named("jsBrowserProductionWebpack")) {
        into("static")
        exclude("webpack.config.js")
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

/*
   Run, image and deploy tasks. Every path is relative to the regatta directory (this project's
   parent), which is also the image's build context, so they work from either root.
*/
val regattaDir: File = projectDir.parentFile

val standaloneBuild: Boolean = rootProject.projectDir == regattaDir
fun Task.requireStandaloneBuild() = doFirst {
    if (!standaloneBuild) throw GradleException(
        "$path must run from regatta's own build, not the parent repository's: " +
            "the web bundle built here is missing its Tailwind utilities. " +
            "Run: cd ${regattaDir.relativeToOrSelf(rootProject.projectDir)} && ./gradlew $path"
    )
}

tasks.register<Exec>("run") {
    group = "application"
    description = "Build the distribution and run the server (needs the database up: database/podman-compose up)."
    dependsOn("installJvmDist")
    workingDir = regattaDir
    commandLine("bash", "-c", "./server/build/install/regatta-jvm/bin/regatta")
}

val makeImg by tasks.registering(Exec::class) {
    description = "Build the container image ghcr.io/manimaul/regatta:latest."
    dependsOn("installJvmDist")
    requireStandaloneBuild()
    workingDir = regattaDir
    commandLine("bash", "-c", "podman build --platform linux/amd64 -f Containerfile -t ghcr.io/manimaul/regatta:latest .")
}

val pubImg by tasks.registering(Exec::class) {
    description = "Push the container image."
    dependsOn(makeImg)
    requireStandaloneBuild()
    commandLine("bash", "-c", "podman push ghcr.io/manimaul/regatta:latest")
}

fun deployProperty(name: String): String =
    project.findProperty(name)?.toString()
        ?: throw GradleException("$name missing from ~/.gradle/gradle.properties")

fun deploymentYaml(name: String): File {
    val template = File(regattaDir, "k8s_deploy/$name")
    val yaml = template.readText()
        .replace("{REGATTA_PG_USER}", deployProperty("REGATTA_PG_USER"))
        .replace("{REGATTA_PG_PASS}", deployProperty("REGATTA_PG_PASS"))
    return File(template.parentFile, ".___${template.name}").also { it.writeText(yaml) }
}

val k8sApplySecrets by tasks.registering(Exec::class) {
    commandLine("bash", "-c", "kubectl apply -f '${deploymentYaml("secrets.yaml").absolutePath}'")
}

val k8sApplyServer by tasks.registering(Exec::class) {
    mustRunAfter(pubImg, k8sApplySecrets)
    commandLine("bash", "-c", "kubectl apply -f '${deploymentYaml("server.yaml").absolutePath}'")
}

tasks.register<Exec>("k8sDeleteServer") {
    commandLine("bash", "-c", "kubectl delete -f '${deploymentYaml("server.yaml").absolutePath}'")
}

val rolloutRestart by tasks.registering(Exec::class) {
    mustRunAfter(k8sApplyServer, pubImg)
    commandLine("bash", "-c", "kubectl -n mxmariner rollout restart deployment/regatta-deployment && " +
        "kubectl -n mxmariner rollout status deployment/regatta-deployment --timeout=180s")
}

tasks.register("deployServer") {
    group = "deployment"
    description = "Build and push the image, apply the server manifest and roll the pods."
    dependsOn(makeImg, pubImg, k8sApplyServer, rolloutRestart)
}
