package com.mxmariner.regatta.plugins

import com.mxmariner.regatta.auth.Token
import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.Series
import com.mxmariner.regatta.db.RegattaDatabase
import com.mxmariner.regatta.versionedApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/series".versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findSeries(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get("/allSeries".versionedApi()) {
            call.respond(RegattaDatabase.allSeries())
        }
        get("/find/series".versionedApi()) {
            call.request.queryParameters["name"]?.let {
                RegattaDatabase.findSeries(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        post("/login".versionedApi()) {
            val login = call.receive<Login>()
            Token.createLoginResponse(login)?.let {
                call.respond(it)
            } ?: call.respond(HttpStatusCode.Unauthorized)
        }
        authenticate(Token.Admin.name) {
            post("/auth".versionedApi()) {
                val auth = call.receive<AuthRecord>()
                RegattaDatabase.saveAuth(auth)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            post("/person".versionedApi()) {
                val person = call.receive<Person>()
                RegattaDatabase.upsertPerson(person)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            post("/series".versionedApi()) {
                val series = call.receive<Series>()
                RegattaDatabase.upsertSeries(series)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            delete("/person".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deletePerson(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete("/series".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteSeries(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            get("/people".versionedApi()) {
                call.respond(RegattaDatabase.allPeople())
            }
            get("/person".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.findPerson(it)
                }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
            }
            get("/find/person".versionedApi()) {
                call.request.queryParameters["name"]?.let {
                    RegattaDatabase.findPerson(it)
                }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
            }
            get("/about".versionedApi()) {
                call.respondText("Hello, ${call.principal<UserIdPrincipal>()?.name}!")
            }
        }
        staticResources("/", "static", "index.html")
    }
}

