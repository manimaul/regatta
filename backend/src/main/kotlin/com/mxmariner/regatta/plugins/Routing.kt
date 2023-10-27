package com.mxmariner.regatta.plugins

import com.mxmariner.regatta.contentDir
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/api") {
            call.respondText("Hello World!")
        }
        staticFiles("/", contentDir)
    }
}
