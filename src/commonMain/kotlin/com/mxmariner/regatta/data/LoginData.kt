package com.mxmariner.regatta.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class Login(
    val userName: String,
    val hashOfHash: String,
    val salt: String,
    val time: Instant,
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
)
