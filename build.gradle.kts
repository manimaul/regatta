import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.compose") version kotlinVersion apply false
    id("org.jetbrains.compose") version composeVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}


tasks.findByPath(":web:jsBrowserProductionWebpack")?.let { it as? KotlinWebpack }?.apply {
    doFirst {
        File("web/webpack.config.d/dev_server_config.js")
            .renameTo(File("web/webpack.config.d/dev_server_config"))
    }
    doLast {
        File("web/webpack.config.d/dev_server_config")
            .renameTo(File("web/webpack.config.d/dev_server_config.js"))
    }
}

tasks.findByPath(":server:installDist")?.apply {
    dependsOn(":web:jsBrowserProductionWebpack")
    mustRunAfter(":web:jsBrowserProductionWebpack")
}


tasks.findByPath(":server:jvmProcessResources")?.let { it as? Copy }?.apply {
    tasks.findByPath(":web:jsBrowserProductionWebpack")?.let {
        from(it) {
            into("static")
        }
        exclude("webpack.config.js")
    }
}

task<Exec>("makeImg") {
    dependsOn(":server:installDist")
    mustRunAfter(":server:installDist")
    commandLine("bash", "-c", "docker build -t ghcr.io/manimaul/regatta:latest .")
}

task<Exec>("pubImg") {
    dependsOn(":makeImg")
    mustRunAfter(":makeImg")
    commandLine("bash", "-c", "docker push ghcr.io/manimaul/regatta:latest")
}

task<Exec>("k8sApplyServer") {
    dependsOn(":pubImg")
    mustRunAfter(":pubImg")
    commandLine("bash", "-c", "kubectl apply -f '${serverYaml().absolutePath}'")
}

task<Exec>("k8sDeleteServer") {
    commandLine("bash", "-c", "kubectl delete -f '${serverYaml().absolutePath}'")
}

task<Exec>("k8sApplyDatabase") {
    commandLine("bash", "-c", "kubectl apply -f '${dbYaml().absolutePath}'")
}

task<Exec>("k8sDeleteDatabase") {
    mustRunAfter(":pubImg")
    commandLine("bash", "-c", "kubectl delete -f '${dbYaml().absolutePath}'")
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

fun dbYaml() : File {
    val pgUser = getProperty("REGATTA_PG_USER")
    val pgPass = getProperty("REGATTA_PG_PASS")
    val root = File(rootProject.projectDir, "k8s_deploy/database.yaml")
    return deploymentYaml(root, pgUser, pgPass)
}

fun serverYaml() : File {
    val pgUser = getProperty("REGATTA_PG_USER")
    val pgPass = getProperty("REGATTA_PG_PASS")
    val root = File(rootProject.projectDir, "k8s_deploy/server.yaml")
    return deploymentYaml(root, pgUser, pgPass)
}

fun deploymentYaml(root: File, pgUser: String, pgPass: String): File {
    val yaml = root.inputStream()
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
    return File(root.parentFile, ".___${root.name}").also {
        it.writeText(yaml)
    }
}

