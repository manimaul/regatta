package com.mxmariner.regatta

import com.mxmariner.regatta.plugins.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*

fun main() {
    embeddedServer(Netty, port = 8888, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRouting()
    install(ContentNegotiation) {
        json()
    }
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
