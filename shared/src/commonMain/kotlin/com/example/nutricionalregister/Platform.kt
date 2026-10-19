package com.example.nutricionalregister

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform