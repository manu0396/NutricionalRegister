package com.example.nutricionalregister.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

internal fun createProductDatabase(): ProductDatabase = ProductDatabase(createProductDatabaseDriver())

internal fun createProductHttpClient(): HttpClient =
    HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }

private fun createProductDatabaseDriver(): NativeSqliteDriver =
    NativeSqliteDriver(
        schema = ProductDatabase.Schema,
        name = DATABASE_NAME,
    )

private const val DATABASE_NAME = "product-search.db"
private const val REQUEST_TIMEOUT_MILLIS = 10_000L
