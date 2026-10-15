package com.mxmariner.regatta.plugins

import com.mxmariner.regatta.resourceAsString
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.sql.SQLException

/** SQLSTATE class 23503: a delete or update would leave a foreign key dangling. */
private const val FOREIGN_KEY_VIOLATION = "23503"

private fun Throwable.isForeignKeyViolation(): Boolean =
    generateSequence(this) { it.cause?.takeIf { cause -> cause !== it } }
        .filterIsInstance<SQLException>()
        .any { it.sqlState == FOREIGN_KEY_VIOLATION }

/**
 * Error responses never carry exception text: the detail (SQL, table names, ids) goes to the server log and the client
 * gets a generic message. A foreign key violation is the client's doing (deleting something still in use), so it is
 * a 409 rather than a 500. Unmatched routes serve the SPA so client-side routing works.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val request = "${call.request.httpMethod.value} ${call.request.path()}"
            if (cause.isForeignKeyViolation()) {
                call.application.log.warn("$request rejected: record is still referenced by other records")
                call.respondText(
                    "This record is still referenced by other records and cannot be deleted.",
                    status = HttpStatusCode.Conflict,
                )
            } else {
                call.application.log.error("$request failed", cause)
                call.respondText("Internal server error", status = HttpStatusCode.InternalServerError)
            }
        }
        unhandled { call ->
            call.respondText(resourceAsString("static/index.html") ?: "", contentType = ContentType.Text.Html)
        }
    }
}
