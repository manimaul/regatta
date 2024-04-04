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
        get(ApiPaths.series.versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findSeries(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get(ApiPaths.allSeries.versionedApi()) {
            call.respond(RegattaDatabase.allSeries())
        }
        get(ApiPaths.bracket.versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findBracket(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get(ApiPaths.allClasses.versionedApi()) {
            call.respond(RegattaDatabase.allClasses())
        }
        get(ApiPaths.standings.versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let { id ->
                call.request.queryParameters["year"]?.toInt()?.let { year ->
                    RaceResultReporter.getStandingsReport(id, year)?.let {
                        call.respond(it)
                    }
                }
            } ?: call.respond(HttpStatusCode.NoContent)
        }
        get(ApiPaths.findSeries.versionedApi()) {
            call.request.queryParameters["name"]?.let {
                RegattaDatabase.findSeries(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get(ApiPaths.raceClass.versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findClass(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        post(ApiPaths.login.versionedApi()) {
            val login = call.receive<Login>()
            Token.createLoginResponse(login)?.let {
                call.respond(it)
            } ?: call.respond(HttpStatusCode.Unauthorized)
        }
        get(ApiPaths.years.versionedApi()) {
            call.respond(RegattaDatabase.allYears())
        }
        get(ApiPaths.allRaces.versionedApi()) {
            call.request.queryParameters["year"]?.toIntOrNull()?.let { year ->
                call.respond(RegattaDatabase.allRaces(year))
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
        get(ApiPaths.race.versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findRace(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get(ApiPaths.raceSchedule.versionedApi()) {
            call.request.queryParameters["id"]?.toLong()?.let {
                RegattaDatabase.findRaceSchedule(it)
            }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
        }
        get(ApiPaths.results.versionedApi()) {
            val results = call.request.queryParameters["year"]?.toIntOrNull()?.let {
                RegattaDatabase.getResults(it)
            } ?: call.request.queryParameters["raceId"]?.toLong()?.let {
                RegattaDatabase.resultsByRaceId(it)
            } ?: RegattaDatabase.allResults()
            call.respond(results)
        }
        get(ApiPaths.resultCount.versionedApi()) {
            call.request.queryParameters["raceId"]?.toLong()?.let {
                call.respond(RegattaDatabase.resultCount(it))
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
        get(ApiPaths.report.versionedApi()) {
            call.request.queryParameters["raceId"]?.toLong()?.let {
                RaceResultReporter.getReport(it)?.let { report ->
                    call.respond(report)
                } ?: call.respond(HttpStatusCode.NoContent)
            }
            call.respond(HttpStatusCode.NoContent)
        }
        authenticate(Token.Admin.name) {
            post(ApiPaths.raceSchedule.versionedApi()) {
                val cs = call.receive<RaceSchedule>()
                RegattaDatabase.insertSchedule(cs)?.let { call.respond(it) } ?: call.respond(HttpStatusCode.BadRequest)
            }
            delete(ApiPaths.results.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteResult(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            post(ApiPaths.results.versionedApi()) {
                val result = call.receive<RaceResult>()
                RegattaDatabase.upsertResult(result)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.Conflict)
            }
            post(ApiPaths.race.versionedApi()) {
                val race = call.receive<Race>()
                RegattaDatabase.upsertRace(race)?.let { call.respond(it) } ?: call.respond(HttpStatusCode.Conflict)
            }
            post(ApiPaths.auth.versionedApi()) {
                val auth = call.receive<AuthRecord>()
                RegattaDatabase.saveAuth(auth)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            post(ApiPaths.skipper.versionedApi()) {
                val person = call.receive<Person>()
                RegattaDatabase.upsertPerson(person)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.Conflict)
            }
            post(ApiPaths.series.versionedApi()) {
                val series = call.receive<List<Series>>()
                call.respond(RegattaDatabase.upsertSeries(series))
            }
            delete(ApiPaths.skipper.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deletePerson(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete(ApiPaths.race.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteRace(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete(ApiPaths.raceClass.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteClass(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete(ApiPaths.series.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteSeries(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            delete(ApiPaths.bracket.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteBracket(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
            post(ApiPaths.raceClass.versionedApi()) {
                val body = call.receive<List<RaceClass>>()
                RegattaDatabase.upsertClass(body).let {
                    call.respond(it)
                }
            }
            post(ApiPaths.bracket.versionedApi()) {
                val body = call.receive<Bracket>()
                RegattaDatabase.upsertBracket(body)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            get(ApiPaths.allSkippers.versionedApi()) {
                call.respond(RegattaDatabase.allPeople())
            }
            get(ApiPaths.skipper.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.findPerson(it)
                }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
            }
            get(ApiPaths.findSkipper.versionedApi()) {
                call.request.queryParameters["name"]?.let {
                    RegattaDatabase.findPerson(it)
                }?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NoContent)
            }
            get(ApiPaths.allBoats.versionedApi()) {
                call.respond(RegattaDatabase.allBoats())
            }
            get(ApiPaths.boat.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.findBoat(it)?.let { boat ->
                        call.respond(boat)
                    }
                } ?: call.request.queryParameters["person_id"]?.toLong()?.let {
                    RegattaDatabase.findBoatForPerson(it)?.let { boat ->
                        call.respond(boat)
                    } ?: call.respond(HttpStatusCode.NoContent)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            post(ApiPaths.boat.versionedApi()) {
                val boat = call.receive<Boat>()
                RegattaDatabase.upsertBoat(boat)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
            delete(ApiPaths.boat.versionedApi()) {
                call.request.queryParameters["id"]?.toLong()?.let {
                    RegattaDatabase.deleteBoat(it)
                }?.let { call.respond(HttpStatusCode.OK) } ?: call.respond(HttpStatusCode.NoContent)
            }
        }
        staticResources("/", "static", "index.html")
    }
}

