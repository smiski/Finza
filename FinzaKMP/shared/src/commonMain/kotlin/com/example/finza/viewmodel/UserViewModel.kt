// Caleb Mercier - 2025
// Completed with assistance from Chat GPT & Gemini
package com.example.finza.viewmodel

import com.example.finza.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel {

    private val api = ApiClient()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _greeting = MutableStateFlow("")
    val greeting: StateFlow<String> = _greeting

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    fun signup(username: String, password: String) {
        scope.launch {
            val u = username.trim()
            val p = password.trim()
            try {
                println("DEBUG SIGNUP username='$u'")
                val message = api.signup(u, p)
                _greeting.value = message
            } catch (e: Exception) {
                _greeting.value = "Sign up failed: ${e.message}"
            }
        }
    }

    fun login(username: String, password: String) {
        scope.launch {
            val u = username.trim()
            val p = password.trim()
            try {
                println("DEBUG LOGIN username='$u'")
                val res = api.login(u, p)
                _token.value = res.access_token
                _greeting.value = res.message ?: "Login successful"
            } catch (e: Exception) {
                _greeting.value = "Login failed: ${e.message}"
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}