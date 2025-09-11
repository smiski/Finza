package com.example.finza

import com.example.finza.network.jsonSerializer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

actual fun getHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(jsonSerializer)
    }
}