// Caleb Mercier - 2025
// Completed with assistance from Chat GPT & Gemini
package com.example.finza.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// For emulator -> host machine FastAPI use 10.0.2.2
// private const val BASE_URL = "http://10.0.2.2:8000"
private const val BASE_URL = "http://10.0.0.149:8000"

class ApiClient {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                    isLenient = true
                }
            )
        }
    }

    // ---------- Auth ----------
    @Serializable
    data class SignupResponse(
        val message: String? = null
    )

    @Serializable
    data class RegisterRequest(val username: String, val password: String)

    @Serializable
    data class LoginRequest(val username: String, val password: String)

    @Serializable
    data class LoginResponse(
        val message: String? = null,
        val access_token: String? = null
    )

    suspend fun signup(username: String, password: String): String {
        val response = client.post("$BASE_URL/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, password))
        }

        val bodyText = response.bodyAsText()
        return try {
            val parsed = Json.decodeFromString(SignupResponse.serializer(), bodyText)
            parsed.message ?: "User $username registered"
        } catch (_: Exception) {
            "Sign up response: $bodyText"
        }
    }

    suspend fun login(username: String, password: String): LoginResponse {
        val response = client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }

        val bodyText = response.bodyAsText()
        return try {
            Json.decodeFromString(LoginResponse.serializer(), bodyText)
        } catch (e: Exception) {
            throw RuntimeException("Login parse error: ${e.message}. Body was: $bodyText")
        }
    }

    // ---------- Monte Carlo DTOs (match Python monte_carlo.py) ----------
    @Serializable
    data class AssetReturn(val mean: Double, val volatility: Double)

    @Serializable
    data class Returns(
        val cash: AssetReturn,
        val taxable: AssetReturn,
        val tax_deferred: AssetReturn,
        val tax_free: AssetReturn
    )

    @Serializable
    data class Portfolio(
        val cash: Double,
        val taxable: Double,
        val tax_deferred: Double,
        val tax_free: Double
    )

    @Serializable
    data class AnnualSavings(
        val cash: Double,
        val taxable: Double,
        val tax_deferred: Double,
        val tax_free: Double
    )

    @Serializable
    data class Individual(
        val name: String,
        val current_age: Int,
        val retirement_age: Int,
        val planning_horizon: Int,
        val pre_retirement_income: Double,
        val post_retirement_income: Double,
        val portfolio: Portfolio,
        val annual_savings: AnnualSavings
    )

    @Serializable
    data class RetirementInput(
        val individuals: List<Individual>,
        val pre_retirement_expenses: Double,
        val post_retirement_expenses: Double,
        val returns: Returns,
        val inflation: Double = 0.025,
        val num_simulations: Int = 1000
    )

    // flat response to match monte_carlo.py return value
    @Serializable
    data class SimulationResponse(
        val years: List<Int>,
        val total_balances: List<Double>,
        val success_probability: Double,
        val average_ending_balance: Double,
        val ending_balance_plus_1sd: Double,
        val ending_balance_minus_1sd: Double,
        val average_funds_last_age: Double,
        val ages_per_year: List<List<String>>
    )

    // ---------- Recommendations DTOs (match /recommendations) ----------
    @Serializable
    data class Recommendation(
        val id: Int,
        val title: String,
        val description: String,
        val impact_summary: String
    )

    @Serializable
    data class RecommendationResponse(
        val recommendations: List<Recommendation>
    )

    // ---------- Calls ----------
    suspend fun runSimulation(
        token: String,
        input: RetirementInput
    ): SimulationResponse {
        val response = client.post("$BASE_URL/simulate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(input)
        }

        val bodyText = response.bodyAsText()
        return try {
            Json.decodeFromString(SimulationResponse.serializer(), bodyText)
        } catch (e: Exception) {
            throw RuntimeException("Simulation parse error: ${e.message}. Body was: $bodyText")
        }
    }

    /**
     * NEW: call the /recommendations endpoint.
     *
     * Backend expects the SAME RetirementInput payload as /simulate,
     * not a simulation_id. It returns { "recommendations": [ ... ] }.
     */
    suspend fun getRecommendations(
        token: String,
        input: RetirementInput
    ): RecommendationResponse {
        val response = client.post("$BASE_URL/recommendations") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(input)
        }

        val bodyText = response.bodyAsText()
        return try {
            Json.decodeFromString(RecommendationResponse.serializer(), bodyText)
        } catch (e: Exception) {
            throw RuntimeException("Recommendations parse error: ${e.message}. Body was: $bodyText")
        }
    }
}