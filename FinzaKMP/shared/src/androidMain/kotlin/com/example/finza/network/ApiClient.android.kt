package com.example.finza.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

actual fun getHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(jsonSerializer)
    }
}