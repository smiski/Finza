package com.example.finza

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val message: String, val access_token: String? = null)