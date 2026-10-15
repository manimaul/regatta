package com.mxmariner.regatta

import com.mxmariner.regatta.auth.Token
import com.mxmariner.regatta.db.RegattaDatabase
import com.mxmariner.regatta.plugins.configureRouting
import com.mxmariner.regatta.plugins.configureStatusPages
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import java.util.*

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    embeddedServer(Netty, port = 8888, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    RegattaDatabase.init()
    install(Compression)
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
    configureStatusPages()
}

fun Any.resourceAsString(name: String): String? {
    return javaClass.classLoader.getResourceAsStream(name)?.let {
        it.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }
}
