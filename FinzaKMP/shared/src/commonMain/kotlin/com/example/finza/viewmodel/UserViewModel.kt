package com.example.finza.viewmodel

import com.example.finza.network.getHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class UserViewModel {
    private val client: HttpClient = getHttpClient()
    private val _greeting = MutableStateFlow("")
    val greeting: StateFlow<String> = _greeting

    private val scope = CoroutineScope(Dispatchers.Default)

    fun signup(username: String, password: String) {
        scope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:8000/signup") {
                    contentType(ContentType.Application.Json)
                    setBody(SignupRequest(username, password))
                }
                if (response.status.isSuccess()) {
                    _greeting.value = "User $username created!"
                } else {
                    _greeting.value = "Signup failed: ${response.status}"
                }
            } catch (e: Exception) {
                _greeting.value = "Error: ${e.message}"
            }
        }
    }

    fun login(username: String, password: String) {
        scope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:8000/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(username, password))
                }
                if (response.status.isSuccess()) {
                    val text = response.bodyAsText()
                    _greeting.value = "Hello $username!"
                } else {
                    _greeting.value = "Login failed: ${response.status}"
                }
            } catch (e: Exception) {
                _greeting.value = "Error: ${e.message}"
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class SignupRequest(val username: String, val password: String)

    @kotlinx.serialization.Serializable
    data class LoginRequest(val username: String, val password: String)
}