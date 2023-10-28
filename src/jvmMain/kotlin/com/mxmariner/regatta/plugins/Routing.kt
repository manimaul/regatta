package com.mxmariner.regatta.plugins

import com.mxmariner.regatta.data.AboutInfo
import com.mxmariner.regatta.versionedApi
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/about".versionedApi()) {
            call.respond(AboutInfo("1.0"))
        }
        staticResources("/", "static", "index.html")
    }
}

