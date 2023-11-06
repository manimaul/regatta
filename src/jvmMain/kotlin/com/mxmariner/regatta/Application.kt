package com.mxmariner.regatta

import com.mxmariner.regatta.auth.Token
import com.mxmariner.regatta.db.RegattaDatabase
import com.mxmariner.regatta.plugins.configureRouting
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun main() {
    embeddedServer(Netty, port = 8888, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    RegattaDatabase.init()
    install(Authentication) {
        bearer(Token.Admin.name) {
            realm = Token.Admin.realm
            authenticate { tokenCredential ->
                Token.validateAdminToken(tokenCredential.token)
            }
        }
    }
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
