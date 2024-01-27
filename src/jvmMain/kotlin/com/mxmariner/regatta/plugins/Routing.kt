package com.mxmariner.regatta.plugins

import com.mxmariner.regatta.auth.Token
import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import com.mxmariner.regatta.results.RaceResultReporter
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
        get("/allClasses".versionedApi()) {
            call.respond(RegattaDatabase.allRaceClasses())
        }
        get("/raceClass".versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findRaceClass(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get("/allCategories".versionedApi()) {
            call.respond(RegattaDatabase.allCategories())
        }
        get("/find/series".versionedApi()) {
            call.request.queryParameters["name"]?.let {
                RegattaDatabase.findSeries(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get("/raceCategory".versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findRaceCategory(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        post("/login".versionedApi()) {
            val login = call.receive<Login>()
            Token.createLoginResponse(login)?.let {
                call.respond(it)
            } ?: call.respond(HttpStatusCode.Unauthorized)
        }
        get("/allRaces".versionedApi()) {
            call.respond(RegattaDatabase.allRaces())
        }
        get("/races".versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findRace(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get("/results".versionedApi()) {
            val results = call.request.queryParameters["year"]?.toIntOrNull()?.let {
                RegattaDatabase.getResults(it)
            } ?: call.request.queryParameters["raceId"]?.toLong()?.let {
                RegattaDatabase.resultsByRaceId(it)
            } ?: RegattaDatabase.allResults()
            call.respond(results)
        }
        get("/report".versionedApi()) {
            call.request.queryParameters["raceId"]?.toLong()?.let {
               RaceResultReporter.getReport(it)?.let { report ->
                   call.respond(report)
               } ?: call.respond(HttpStatusCode.NoContent)
            }
            call.respond(HttpStatusCode.NoContent)
        }
        authenticate(Token.Admin.name) {
            delete("/results".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteResult(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            post("/results".versionedApi()) {
                val result = call.receive<RaceResult>()
                RegattaDatabase.upsertResult(result)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.Conflict)
            }
            post("/races".versionedApi()) {
                val race = call.receive<Race>()
                RegattaDatabase.upsertRace(race)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.Conflict)
            }
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
                } ?: call.respond(HttpStatusCode.Conflict)
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
            delete("/races".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteRace(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete("/category".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteCategory(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete("/series".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteSeries(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete("/raceClass".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteRaceClass(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            post("/raceCategory".versionedApi()) {
                val body = call.receive<RaceClassCat>()
                RegattaDatabase.upsertRaceCategory(body)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            post("/raceClass".versionedApi()) {
                val body = call.receive<RaceClass>()
                RegattaDatabase.upsertRaceClass(body)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
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
            get("/allBoats".versionedApi()) {
                call.respond(RegattaDatabase.allBoats())
            }
            get("/boat".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.findBoat(it)?.let {  boat ->
                        call.respond(boat)
                    }
                } ?: call.request.queryParameters["person_id"]?.toLong()?.let {
                    RegattaDatabase.findBoatForPerson(it)?.let { boat ->
                        call.respond(boat)
                    } ?: call.respond(HttpStatusCode.NoContent)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            post("/boat".versionedApi()) {
                val boat = call.receive<Boat>()
                RegattaDatabase.upsertBoat(boat)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            delete("/boat".versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteBoat(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
        }
        staticResources("/", "static", "index.html")
    }
}

