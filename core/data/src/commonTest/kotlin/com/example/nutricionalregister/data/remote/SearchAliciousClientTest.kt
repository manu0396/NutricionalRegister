package com.example.nutricionalregister.data.remote

import com.example.nutricionalregister.domain.product.SearchTerm
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchAliciousClientTest {
    @Test
    fun sendsAnExplicitEncodedQueryAndRequiredFields() =
        runTest {
            var captured: HttpRequestData? = null
            val httpClient =
                searchAliciousTestClient { request ->
                    captured = request
                    jsonResponse(SearchAliciousResponseFixtures.oneProduct)
                }
            val client = SearchAliciousClient(httpClient, "https://search.example")

            val products = client.search(SearchTerm.fromInput("Oat drink & cereal")!!)

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/search", request.url.encodedPath)
            assertEquals("oat drink & cereal", request.url.parameters["q"])
            assertContains(request.url.encodedQuery, "%26")
            assertEquals("1", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["page_size"])
            assertTrue(
                request.url.parameters["fields"]
                    .orEmpty()
                    .contains("product_name"),
            )
            assertEquals(listOf("0012345678905"), products.map { it.code })
            httpClient.close()
        }

    @Test
    fun preservesServiceOrderAndAcceptsEmptyResults() =
        runTest {
            val response =
                """
                {
                  "hits": [
                    {"code":"first","product_name":"First"},
                    {"code":"second","product_name":"Second"}
                  ]
                }
                """.trimIndent()
            val client = SearchAliciousClient(searchAliciousTestClient { jsonResponse(response) }, "https://search.example")
            val emptyClient =
                SearchAliciousClient(
                    searchAliciousTestClient { jsonResponse(SearchAliciousResponseFixtures.noProducts) },
                    "https://search.example",
                )

            assertEquals(
                listOf("First", "Second"),
                client.search(SearchTerm.fromInput("oat")!!).map { it.name },
            )
            assertTrue(emptyClient.search(SearchTerm.fromInput("missing")!!).isEmpty())
        }
}
