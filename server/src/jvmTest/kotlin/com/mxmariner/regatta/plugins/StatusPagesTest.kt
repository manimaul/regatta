package com.mxmariner.regatta.plugins

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StatusPagesTest {

    private fun ApplicationTestBuilder.failingRoutes() {
        application {
            configureStatusPages()
            routing {
                get("/boom") { throw IllegalStateException("secret detail: table raceresults id 86") }
                get("/fk") {
                    throw RuntimeException(
                        "wrapped",
                        SQLException("""update or delete on table "race" violates foreign key constraint""", "23503"),
                    )
                }
            }
        }
    }

    @Test
    fun unexpectedErrorIsAGeneric500() = testApplication {
        failingRoutes()
        val response = client.get("/boom")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertEquals("Internal server error", body)
        assertFalse(body.contains("secret"), "exception text must not reach the client")
    }

    @Test
    fun foreignKeyViolationIsAGeneric409() = testApplication {
        failingRoutes()
        val response = client.get("/fk")
        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = response.bodyAsText()
        assertFalse(body.contains("raceresults") || body.contains("constraint"), "database detail must not reach the client")
        assertEquals("This record is still referenced by other records and cannot be deleted.", body)
    }
}
