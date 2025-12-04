// Caleb Mercier - 2025
// Completed with assistance from Chat GPT & Gemini
package com.example.finza

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finza.viewmodel.UserViewModel

enum class Screen {
    Auth,
    Planner
}

@Composable
fun App() {
    val userViewModel = remember { UserViewModel() }

    var currentScreen by remember { mutableStateOf(Screen.Auth) }

    val greeting by userViewModel.greeting.collectAsState()
    val token by userViewModel.token.collectAsState()

    // When we successfully get a token, stay on Auth but show planner button.
    // When user taps it, switch to Planner screen.
    when (currentScreen) {
        Screen.Auth -> AuthScreen(
            greeting = greeting,
            token = token,
            onSignup = { u, p -> userViewModel.signup(u, p) },
            onLogin = { u, p -> userViewModel.login(u, p) },
            onStartPlanner = {
                if (token != null) {
                    currentScreen = Screen.Planner
                }
            }
        )
        Screen.Planner -> RetirementPlannerScreen(
            token = token,
            onBack = { currentScreen = Screen.Auth }
        )
    }
}

@Composable
private fun AuthScreen(
    greeting: String,
    token: String?,
    onSignup: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onStartPlanner: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FINZA",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = Color(0xFFB388FF) // Light violet
            )
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { onSignup(username, password) }) {
                Text("Sign Up")
            }
            Button(onClick = { onLogin(username, password) }) {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (greeting.isNotEmpty()) {
            Text(greeting, style = MaterialTheme.typography.headlineSmall)
        }

        if (token != null) {
            Spacer(Modifier.height(16.dp))
            Text("Logged in ✓", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onStartPlanner) {
                Text("Start Retirement Planner")
            }
        }
    }
}