package com.example.finza.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class UserSignup(val username: String, val password: String)

@Serializable
data class UserLogin(val username: String, val password: String)

@Serializable
data class LoginResponse(val message: String, val access_token: String? = null)

class ApiService(private val client: HttpClient, private val baseUrl: String) {

    suspend fun signup(username: String, password: String): String {
        val response: HttpResponse = client.post("$baseUrl/signup") {
            contentType(ContentType.Application.Json)
            setBody(UserSignup(username, password))
        }
        return response.bodyAsText()
    }

    suspend fun login(username: String, password: String): LoginResponse {
        return client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLogin(username, password))
        }.body()
    }
}