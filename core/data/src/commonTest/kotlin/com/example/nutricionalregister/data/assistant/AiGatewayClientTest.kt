package com.example.nutricionalregister.data.assistant

import com.example.nutricionalregister.domain.assistant.AssistantAction
import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantLibraryFood
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantRole
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiGatewayClientTest {
    @Test
    fun `the conversation is posted to the chat endpoint with the minimal context`() =
        runTest {
            var captured: HttpRequestData? = null
            val httpClient =
                aiGatewayTestClient { request ->
                    captured = request
                    gatewayResponse("""{"reply":"Understood","actions":[]}""")
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            client.complete(
                messages = listOf(AssistantMessage(AssistantRole.USER, "What should I eat?")),
                context = context,
            )

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://gateway.example/v1/chat", request.url.toString())
            assertEquals("/v1/chat", request.url.encodedPath)
            assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
            assertEquals(EXPECTED_REQUEST_BODY, request.body.toByteArray().decodeToString())
        }

    @Test
    fun `the reply and every action kind are decoded`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    gatewayResponse(
                        """
                        {
                          "reply": "I can log that for you.",
                          "actions": [
                            {"type": "log", "productCode": "00042", "quantityGrams": 150.0, "meal": "DINNER"},
                            {"type": "remove", "entryId": 7},
                            {"type": "change", "entryId": 8, "quantityGrams": 200.0, "meal": "LUNCH"}
                          ]
                        }
                        """.trimIndent(),
                    )
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val reply =
                client.complete(
                    messages = listOf(AssistantMessage(AssistantRole.USER, "Log my oat drink")),
                    context = context,
                )

            assertEquals("I can log that for you.", reply.text)
            assertEquals(
                listOf(
                    AssistantAction.LogFood(
                        productCode = "00042",
                        quantityGrams = 150.0,
                        mealType = MealType.DINNER,
                    ),
                    AssistantAction.RemoveEntry(entryId = 7L),
                    AssistantAction.ChangeEntry(
                        entryId = 8L,
                        quantityGrams = 200.0,
                        mealType = MealType.LUNCH,
                    ),
                ),
                reply.actions,
            )
        }

    @Test
    fun `a change that touches only the entry is honoured`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    gatewayResponse(
                        """
                        {
                          "reply": "I can change that entry.",
                          "actions": [
                            {"type": "change", "entryId": 9}
                          ]
                        }
                        """.trimIndent(),
                    )
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val reply =
                client.complete(
                    messages = listOf(AssistantMessage(AssistantRole.USER, "Change the last entry")),
                    context = context,
                )

            assertEquals(
                listOf(AssistantAction.ChangeEntry(entryId = 9L, quantityGrams = null, mealType = null)),
                reply.actions,
            )
        }

    @Test
    fun `an action that names its kind in the kind field is honoured`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    gatewayResponse(
                        """
                        {
                          "reply": "I can log that for you.",
                          "actions": [
                            {"kind": "log", "productCode": "00042", "quantityGrams": 150.0, "meal": "DINNER"}
                          ]
                        }
                        """.trimIndent(),
                    )
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val reply =
                client.complete(
                    messages = listOf(AssistantMessage(AssistantRole.USER, "Log my oat drink")),
                    context = context,
                )

            assertEquals(
                listOf(
                    AssistantAction.LogFood(
                        productCode = "00042",
                        quantityGrams = 150.0,
                        mealType = MealType.DINNER,
                    ),
                ),
                reply.actions,
            )
        }

    @Test
    fun `an action the app cannot honour is dropped and the reply survives`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    gatewayResponse(
                        """
                        {
                          "reply": "Here is what I understood.",
                          "actions": [
                            {"type": "teleport", "productCode": "00042"},
                            {"type": "LOG\u00e9", "productCode": "00042", "quantityGrams": 150.0, "meal": "DINNER"},
                            {"productCode": "00042", "quantityGrams": 150.0, "meal": "DINNER"},
                            {"type": "log", "quantityGrams": 150.0, "meal": "DINNER"},
                            {"type": "log", "productCode": "00042", "quantityGrams": -5.0, "meal": "DINNER"},
                            {"type": "log", "productCode": "00042", "quantityGrams": 150.0, "meal": "MIDNIGHT"},
                            {"type": "log", "productCode": "00042", "quantityGrams": 150.0},
                            {"type": "log", "productCode": "00042", "meal": "DINNER"},
                            {"type": "remove"},
                            {"type": "remove", "entryId": 0},
                            {"type": "change", "entryId": 8, "quantityGrams": 0.0},
                            {"type": "change", "entryId": 0},
                            {"type": "change"}
                          ]
                        }
                        """.trimIndent(),
                    )
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val reply =
                client.complete(
                    messages = listOf(AssistantMessage(AssistantRole.USER, "Log my oat drink")),
                    context = context,
                )

            assertEquals("Here is what I understood.", reply.text)
            assertTrue(reply.actions.isEmpty())
        }

    @Test
    fun `a failed status is reported as a gateway failure`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    gatewayResponse("""{"reply":"","actions":[]}""", HttpStatusCode.ServiceUnavailable)
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val failure =
                assertFailsWith<AiGatewayException> {
                    client.complete(
                        messages = listOf(AssistantMessage(AssistantRole.USER, "Hello")),
                        context = context,
                    )
                }

            assertContains(failure.message.orEmpty(), "503")
        }

    @Test
    fun `a failed status is reported as a gateway failure when the client validates the response`() =
        runTest {
            val httpClient =
                aiGatewayTestClientValidatingResponses {
                    gatewayResponse("""{"reply":"","actions":[]}""", HttpStatusCode.ServiceUnavailable)
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val failure =
                assertFailsWith<AiGatewayException> {
                    client.complete(
                        messages = listOf(AssistantMessage(AssistantRole.USER, "Hello")),
                        context = context,
                    )
                }

            assertContains(failure.message.orEmpty(), "503")
        }

    @Test
    fun `a transport failure is reported as a gateway failure`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    throw IllegalStateException("the network is not there")
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val failure =
                assertFailsWith<AiGatewayException> {
                    client.complete(
                        messages = listOf(AssistantMessage(AssistantRole.USER, "Hello")),
                        context = context,
                    )
                }

            assertContains(failure.message.orEmpty(), "could not be reached")
        }

    @Test
    fun `a reply the app cannot read is reported as a gateway failure`() =
        runTest {
            val httpClient = aiGatewayTestClient { gatewayResponse("this is not JSON") }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val failure =
                assertFailsWith<AiGatewayException> {
                    client.complete(
                        messages = listOf(AssistantMessage(AssistantRole.USER, "Hello")),
                        context = context,
                    )
                }

            assertContains(failure.message.orEmpty(), "could not read")
        }

    @Test
    fun `a reply with a number the app cannot read is reported as a gateway failure`() =
        runTest {
            val httpClient =
                aiGatewayTestClient {
                    gatewayResponse(
                        """
                        {
                          "reply": "Here is what I understood.",
                          "actions": [
                            {"type": "log", "productCode": "00042", "quantityGrams": 1e999, "meal": "DINNER"}
                          ]
                        }
                        """.trimIndent(),
                    )
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            val failure =
                assertFailsWith<AiGatewayException> {
                    client.complete(
                        messages = listOf(AssistantMessage(AssistantRole.USER, "Log my oat drink")),
                        context = context,
                    )
                }

            assertContains(failure.message.orEmpty(), "could not read")
        }

    @Test
    fun `a blank address disables the assistant and sends no request`() =
        runTest {
            var asked = false
            val httpClient =
                aiGatewayTestClient {
                    asked = true
                    gatewayResponse("""{"reply":"Understood","actions":[]}""")
                }
            val client = AiGatewayClient(httpClient, "   ")

            assertFalse(client.isConfigured)

            val failure =
                assertFailsWith<IllegalStateException> {
                    client.complete(
                        messages = listOf(AssistantMessage(AssistantRole.USER, "Hello")),
                        context = context,
                    )
                }

            assertContains(failure.message.orEmpty(), "not configured")
            assertFalse(asked)
        }

    @Test
    fun `a configured address enables the assistant`() =
        runTest {
            val httpClient = aiGatewayTestClient { gatewayResponse("""{"reply":"","actions":[]}""") }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            assertTrue(client.isConfigured)
        }

    @Test
    fun `the request carries no catalogue search or purchase data`() =
        runTest {
            var captured: HttpRequestData? = null
            val httpClient =
                aiGatewayTestClient { request ->
                    captured = request
                    gatewayResponse("""{"reply":"Understood","actions":[]}""")
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            client.complete(
                messages = listOf(AssistantMessage(AssistantRole.USER, "What did I eat?")),
                context = context,
            )

            val body = requireNotNull(captured).body.toByteArray().decodeToString()
            assertFalse(body.contains("nutritionValues"))
            assertFalse(body.contains("brands"))
            assertFalse(body.contains("imageThumbnailUrl"))
            assertFalse(body.contains("searchTerm"))
            assertFalse(body.contains("purchase"))
            assertFalse(body.contains("savedAt"))
        }

    @Test
    fun `the request carries the entries the goal and the library codes and nothing else`() =
        runTest {
            var captured: HttpRequestData? = null
            val httpClient =
                aiGatewayTestClient { request ->
                    captured = request
                    gatewayResponse("""{"reply":"Understood","actions":[]}""")
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            client.complete(
                messages = listOf(AssistantMessage(AssistantRole.USER, "What did I eat?")),
                context = context,
            )

            val body = requireNotNull(captured).body.toByteArray().decodeToString()
            val payload = Json.parseToJsonElement(body).jsonObject
            assertEquals(setOf("messages", "context"), payload.keys)
            val sent = payload.getValue("context").jsonObject
            assertEquals(setOf("entries", "goal", "library"), sent.keys)
            val codes =
                sent
                    .getValue("library")
                    .jsonArray
                    .map { food ->
                        food
                            .jsonObject
                            .getValue("code")
                            .jsonPrimitive
                            .content
                    }
            assertEquals(listOf("00042", "0012345678905"), codes)
            val entries = sent.getValue("entries").jsonArray
            val entry = entries.single().jsonObject
            assertEquals(
                setOf(
                    "entryId",
                    "productCode",
                    "productName",
                    "meal",
                    "quantityGrams",
                    "energyKcal",
                    "proteinGrams",
                    "carbohydrateGrams",
                    "fatGrams",
                ),
                entry.keys,
            )
            assertEquals("7", entry.getValue("entryId").jsonPrimitive.content)
            assertEquals("00042", entry.getValue("productCode").jsonPrimitive.content)
            assertEquals("BREAKFAST", entry.getValue("meal").jsonPrimitive.content)
        }

    @Test
    fun `the conversation is posted the same way without the content negotiation plugin`() =
        runTest {
            var captured: HttpRequestData? = null
            val httpClient =
                aiGatewayTestClientWithoutContentNegotiation { request ->
                    captured = request
                    gatewayResponse("""{"reply":"Understood","actions":[]}""")
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")

            client.complete(
                messages = listOf(AssistantMessage(AssistantRole.USER, "What should I eat?")),
                context = context,
            )

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(EXPECTED_REQUEST_BODY, request.body.toByteArray().decodeToString())
        }

    @Test
    fun `a nutrient the catalogue did not report is left out of the request`() =
        runTest {
            var captured: HttpRequestData? = null
            val httpClient =
                aiGatewayTestClient { request ->
                    captured = request
                    gatewayResponse("""{"reply":"Understood","actions":[]}""")
                }
            val client = AiGatewayClient(httpClient, "https://gateway.example")
            val withoutNutrients =
                DiaryEntry(
                    id = 8L,
                    productCode = "00043",
                    productName = "Plain water",
                    mealType = MealType.SNACKS,
                    quantityGrams = 100.0,
                )

            client.complete(
                messages = listOf(AssistantMessage(AssistantRole.USER, "What did I eat?")),
                context = AssistantContext(entries = listOf(withoutNutrients), goal = goal, library = emptyList()),
            )

            val body = requireNotNull(captured).body.toByteArray().decodeToString()
            val payload = Json.parseToJsonElement(body).jsonObject
            val sent = payload.getValue("context").jsonObject
            val entry =
                sent
                    .getValue("entries")
                    .jsonArray
                    .single()
                    .jsonObject
            assertEquals(
                setOf("entryId", "productCode", "productName", "meal", "quantityGrams"),
                entry.keys,
            )
            assertEquals("100.0", entry.getValue("quantityGrams").jsonPrimitive.content)
        }

    @Test
    fun `an address that is not https is refused`() {
        assertFailsWith<IllegalArgumentException> {
            AiGatewayClient(aiGatewayTestClient { gatewayResponse("""{"reply":"","actions":[]}""") }, "http://gateway.example")
        }
    }
}

private val goal: NutritionGoal =
    NutritionGoal(
        energyKcal = 2000,
        proteinGrams = 120,
        carbohydrateGrams = 220,
        fatGrams = 70,
    )

private val entry: DiaryEntry =
    DiaryEntry(
        id = 7L,
        productCode = "00042",
        productName = "Oat drink",
        mealType = MealType.BREAKFAST,
        quantityGrams = 250.0,
        energyKcal = 105.0,
        proteinGrams = 2.0,
        carbohydrateGrams = 18.0,
        fatGrams = 1.5,
        loggedAtEpochMillis = 1_700_000_000_000L,
        dayEpochMillis = 1_699_920_000_000L,
    )

private val context: AssistantContext =
    AssistantContext(
        entries = listOf(entry),
        goal = goal,
        library =
            listOf(
                AssistantLibraryFood(code = "00042", name = "Oat drink", energyKcal = 100.0),
                AssistantLibraryFood(code = "0012345678905", name = "Rolled oats"),
            ),
    )

private const val EXPECTED_REQUEST_BODY: String =
    """{"messages":[{"role":"USER","text":"What should I eat?"}],""" +
        """"context":{"entries":[{"entryId":7,"productCode":"00042","productName":"Oat drink",""" +
        """"meal":"BREAKFAST","quantityGrams":250.0,"energyKcal":105.0,"proteinGrams":2.0,""" +
        """"carbohydrateGrams":18.0,"fatGrams":1.5}],""" +
        """"goal":{"energyKcal":2000,"proteinGrams":120,"carbohydrateGrams":220,"fatGrams":70},""" +
        """"library":[{"code":"00042","name":"Oat drink","energyKcal":100.0},""" +
        """{"code":"0012345678905","name":"Rolled oats"}]}}"""
