package com.mxmariner.regatta.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Login(
    val userName: String,
    val hashOfHash: String,
    val salt: String,
    val time: Instant,
)

@Serializable
data class AuthRecord(
    val id: Long = 0,
    val hash: String,
    val userName: String,
)

@Serializable
data class LoginResponse(
    val id: Long,
    val hashOfHash: String,
    val salt: String,
    val expires: Instant,
)
