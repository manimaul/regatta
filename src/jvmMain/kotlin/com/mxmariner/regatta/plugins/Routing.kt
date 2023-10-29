package com.mxmariner.regatta.plugins

import com.mxmariner.regatta.data.AboutInfo
import com.mxmariner.regatta.db.RegattaDatabase
import com.mxmariner.regatta.db.Series
import com.mxmariner.regatta.versionedApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        post("/series".versionedApi()) {
            val series = call.receive<Series>()
            RegattaDatabase.upsertSeries(series)?.let {
                call.respond(it)
            } ?: call.respond(HttpStatusCode.InternalServerError)
        }
        get("/series".versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findSeries(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get("/allSeries".versionedApi()) {
            call.respond(RegattaDatabase.allSeries())
        }
        get("/about".versionedApi()) {
            call.respond(AboutInfo("1.0"))
        }
        staticResources("/", "static", "index.html")
    }
}

