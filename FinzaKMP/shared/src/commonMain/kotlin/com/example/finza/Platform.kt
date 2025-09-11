package com.example.finza

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform