package com.example.nutricionalregister

public interface Platform {
    public val name: String
}

public expect fun getPlatform(): Platform
