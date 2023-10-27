package com.mxmariner.regatta.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/api") {
            call.respondText("Hello World!")
        }
        staticResources("/", "static", "index.html")
    }
}
