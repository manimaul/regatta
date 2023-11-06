package com.mxmariner.regatta.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Login(
    val userName: String,
    val hashOfHash: String,
    val salt: String,
    val time: Instant,
    val admin: Boolean,
)

@Serializable
data class AuthRecord(
    val id: Long? = null,
    val hash: String,
    val userName: String,
)

@Serializable
data class LoginResponse(
    val id: Long,
    val hashOfHash: String,
    val salt: String,
    val expires: Instant,
) {
    fun token(): String {
        return "$id:$hashOfHash:$salt:${expires.epochSeconds}"
    }

    companion object {
        fun parseToken(token: String) : LoginResponse? {
            return token.split(":").takeIf { it.size == 4 }?.let {
                val id = it[0].toLongOrNull()
                LoginResponse(
                    id ?: 0L,
                    it[1],
                    it[2],
                    Instant.fromEpochSeconds(it[3].toLongOrNull() ?: 0)
                )
            }
        }
    }
}
