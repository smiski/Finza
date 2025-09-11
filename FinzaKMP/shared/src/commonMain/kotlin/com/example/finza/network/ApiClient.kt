package com.example.finza.network

import io.ktor.client.*
import kotlinx.serialization.json.Json

expect fun getHttpClient(): HttpClient

val jsonSerializer: Json = Json { ignoreUnknownKeys = true }