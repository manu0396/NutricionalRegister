package com.example.nutricionalregister.data.assistant

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A client that answers with [responder] and never reaches a socket.
 */
internal fun aiGatewayTestClient(responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
    HttpClient(MockEngine(responder)) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

/**
 * A client without the content negotiation plugin.
 *
 * The gateway client has to speak the same JSON whether the plugin is
 * installed or not, so the tests exercise both.
 */
internal fun aiGatewayTestClientWithoutContentNegotiation(
    responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient =
    HttpClient(MockEngine(responder)) {
        expectSuccess = false
    }

/**
 * A client that validates the response itself.
 *
 * A client built this way throws when the gateway answers with a failed
 * status instead of handing the response over, so the gateway client has to
 * keep that failure inside its own type.
 */
internal fun aiGatewayTestClientValidatingResponses(
    responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient =
    HttpClient(MockEngine(responder)) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

/**
 * The answer the mocked gateway sends.
 */
internal fun MockRequestHandleScope.gatewayResponse(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
