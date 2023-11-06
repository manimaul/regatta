package com.mxmariner.regatta.auth

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.LoginResponse
import com.mxmariner.regatta.db.RegattaDatabase
import io.ktor.server.auth.*
import io.ktor.utils.io.core.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.io.use
import kotlin.time.Duration.Companion.seconds


object Token {

    private val expiresIn = (60 * 60 * 8).seconds

    object Admin {
        const val name = "admin-auth-bearer"
        const val realm = "admin access"
        val principal = UserIdPrincipal("regatta-admin")
    }

    private fun hashInternal(value: String) : String {
        val md = MessageDigest.getInstance("SHA-512")
        val hash = md.digest(value.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    fun hash(vararg data: String) : String {
        var result = ""
        data.forEach {
            result = hashInternal(it + result)
        }
        return result
    }

    fun timeStampHash(instant: Instant, salt: String, hash: String) : String {
        return hash(salt, "${instant.epochSeconds}", hash)
    }

    fun salt() : String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    suspend fun createLoginResponse(login: Login) : LoginResponse? {
        return RegattaDatabase.getAuth(login.userName)?.let {
            val expected = timeStampHash(login.time, login.salt, it.hash)
            if (expected == login.hashOfHash) {
                val expires = Clock.System.now().plus(expiresIn)
                val salt = salt()
                val hash = timeStampHash(expires, salt, it.hash)
                LoginResponse(
                    id = it.id!!,
                    hashOfHash = hash,
                    salt,
                    expires
                )
            } else {
                null
            }
        }
    }

    private fun validateHash(response: LoginResponse, record: AuthRecord) : Boolean {
        response.expires.takeIf {it.minus(Clock.System.now()).isPositive() }?.let {
            val expected = timeStampHash(response.expires, response.salt, record.hash)
            expected == response.hashOfHash
        }
        return false
    }

    suspend fun validateAdminToken(token: String) : UserIdPrincipal? {
        return if (RegattaDatabase.adminExists()) {
            LoginResponse.parseToken(token)?.let {login ->
                RegattaDatabase.getAuth(login.id)?.let {record ->
                    if (validateHash(login, record)) {
                        Admin.principal
                    } else {
                        null
                    }
                }
            }
        } else {
            Admin.principal
        }
    }
}
