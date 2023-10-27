package com.mxmariner.regatta

import com.mxmariner.regatta.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import java.io.File

fun main() {
    embeddedServer(Netty, port = 8888, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

val contentDir = File("../frontend/build/dist/js/productionExecutable")
fun Application.module() {
    configureRouting()
    install(StatusPages) {

        // https://openenc.com
        unhandled { call ->
            call.respondText(File(contentDir, "index.html").readText(), ContentType.Text.Html)
        }
    }
}
