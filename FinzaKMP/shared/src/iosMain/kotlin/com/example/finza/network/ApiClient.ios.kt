package com.example.finza.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

actual fun getHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) {
        json(jsonSerializer)
    }
}