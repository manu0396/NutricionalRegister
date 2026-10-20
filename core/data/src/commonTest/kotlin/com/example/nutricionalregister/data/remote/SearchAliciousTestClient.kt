package com.example.nutricionalregister.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun searchAliciousTestClient(
    responder: suspend MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> HttpResponseData,
): HttpClient =
    HttpClient(MockEngine(responder)) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

internal fun MockRequestHandleScope.jsonResponse(
    content: String,
    status: io.ktor.http.HttpStatusCode = io.ktor.http.HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
