package com.mxmariner.regatta

import com.mxmariner.regatta.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import java.net.URL
import java.net.URLClassLoader

fun main() {
    embeddedServer(Netty, port = 8888, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRouting()
    install(StatusPages) {
        unhandled { call ->
            call.respondText(resourceAsString("static/index.html") ?: "", contentType = ContentType.Text.Html)
        }
    }
}

fun Any.resourceAsString(name: String): String? {
    return javaClass.classLoader.getResourceAsStream(name)?.let {
        it.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }
}
