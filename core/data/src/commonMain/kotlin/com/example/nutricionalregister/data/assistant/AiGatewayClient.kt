package com.example.nutricionalregister.data.assistant

import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * The client of the AI gateway the app talks to.
 *
 * The gateway is a first-party service the publisher operates. The app only
 * ever knows its address: no credential of any kind reaches the client, so
 * the app cannot leak a key and the service behind the gateway can change
 * without touching the app (ADR-0016).
 *
 * The client speaks exactly one endpoint, `POST {baseUrl}/v1/chat`, and sends
 * the conversation with the minimal context: the entries of the day, the goal
 * and the codes of the user's own library. The catalogue, the recent search
 * terms and the purchase records are never part of the request (FR-028).
 *
 * @property httpClient the client the request is sent through. It should have
 *   the [ContentNegotiation] plugin installed with a JSON format that ignores
 *   unknown keys; without it the client encodes and decodes the same JSON
 *   itself.
 * @property baseUrl the address of the gateway. An empty address disables the
 *   assistant for this build (FR-025), and a configured address must use
 *   HTTPS, because the request carries the user's own diary.
 */
public class AiGatewayClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    /**
     * Whether the gateway address is configured for this build.
     *
     * A build without an address has no assistant: the Coach screen says so
     * plainly instead of showing an input field that cannot work (FR-025).
     */
    public val isConfigured: Boolean = baseUrl.isNotBlank()

    /**
     * The JSON used when the client has no content negotiation plugin.
     */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Whether the client encodes and decodes the payloads itself.
     */
    private val negotiatesContent: Boolean = httpClient.pluginOrNull(ContentNegotiation) != null

    init {
        require(baseUrl.isBlank() || baseUrl.startsWith(HTTPS_PREFIX)) {
            "The AI gateway address must use HTTPS"
        }
    }

    /**
     * Answers the conversation [messages] in the given [context].
     *
     * @throws IllegalStateException when no gateway address is configured.
     * @throws AiGatewayException when the gateway cannot be reached, answers
     *   with a failed status or answers with a reply the app cannot read.
     */
    public suspend fun complete(
        messages: List<AssistantMessage>,
        context: AssistantContext,
    ): AssistantReply {
        check(isConfigured) { NOT_CONFIGURED_MESSAGE }
        val request = gatewayRequest(messages = messages, context = context)
        val response = post(request)
        val payload = read(response)
        return AssistantReply(
            text = payload.reply,
            actions = payload.actions.mapNotNull { action -> action.toAssistantAction() },
        )
    }

    /**
     * Sends the request and makes sure the gateway answered with a success.
     */
    private suspend fun post(request: GatewayRequest): HttpResponse {
        val response =
            try {
                httpClient.post("${baseUrl.trimEnd('/')}$CHAT_PATH") {
                    contentType(ContentType.Application.Json)
                    if (negotiatesContent) {
                        setBody(request)
                    } else {
                        setBody(TextContent(json.encodeToString(request), ContentType.Application.Json))
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: ResponseException) {
                throw AiGatewayException(
                    message = "The AI gateway answered with HTTP ${failure.response.status.value}",
                    cause = failure,
                )
            } catch (failure: Exception) {
                throw AiGatewayException(message = UNREACHABLE_MESSAGE, cause = failure)
            }
        if (!response.status.isSuccess()) {
            throw AiGatewayException("The AI gateway answered with HTTP ${response.status.value}")
        }
        return response
    }

    /**
     * Reads the reply the gateway sent.
     */
    private suspend fun read(response: HttpResponse): GatewayResponse =
        try {
            if (negotiatesContent) {
                response.body()
            } else {
                json.decodeFromString(response.bodyAsText())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw AiGatewayException(message = UNREADABLE_MESSAGE, cause = failure)
        }

    private companion object {
        /** The path the conversation is posted to. */
        const val CHAT_PATH: String = "/v1/chat"

        /** The only scheme the gateway address may use. */
        const val HTTPS_PREFIX: String = "https://"

        /** The reason shown when the build has no gateway address. */
        const val NOT_CONFIGURED_MESSAGE: String = "The assistant is not configured"

        /** The reason shown when the gateway cannot be reached. */
        const val UNREACHABLE_MESSAGE: String = "The AI gateway could not be reached"

        /** The reason shown when the reply cannot be read. */
        const val UNREADABLE_MESSAGE: String = "The AI gateway answered with a reply the app could not read"
    }
}
