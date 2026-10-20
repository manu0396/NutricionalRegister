package com.example.nutricionalregister.data.remote

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductCatalogue
import com.example.nutricionalregister.domain.product.SearchTerm
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

public class SearchAliciousClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val userAgent: String = DEFAULT_USER_AGENT,
) : ProductCatalogue {
    init {
        require(baseUrl.startsWith("https://")) { "Search-a-licious must use HTTPS" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "Page size must be between 1 and $MAX_PAGE_SIZE" }
        require(userAgent.isNotBlank()) { "User-Agent must not be blank" }
    }

    override suspend fun search(term: SearchTerm): List<Product> {
        val response =
            try {
                httpClient.get("${baseUrl.trimEnd('/')}/search") {
                    parameter("q", term.normalizedTerm)
                    parameter("langs", "en")
                    parameter("page_size", pageSize)
                    parameter("page", 1)
                    parameter("fields", REQUESTED_FIELDS)
                    headers {
                        append(HttpHeaders.UserAgent, userAgent)
                    }
                    timeout {
                        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    }
                }
            } catch (exception: HttpRequestTimeoutException) {
                throw SearchAliciousException("Catalogue request timed out", cause = exception)
            }
        if (!response.status.isSuccess()) {
            throw SearchAliciousException(
                message = "Catalogue returned HTTP ${response.status.value}",
                statusCode = response.status.value,
            )
        }
        return ProductResponseMapper.mapResponse(response.bodyAsText())
    }

    public companion object {
        public const val DEFAULT_PAGE_SIZE: Int = 20
        public const val MAX_PAGE_SIZE: Int = 50
        public const val REQUEST_TIMEOUT_MILLIS: Long = 10_000L
        public const val DEFAULT_USER_AGENT: String = "NutriLog/1.0"
        public const val REQUESTED_FIELDS: String =
            "code,product_name,brands,quantity,nutrition_data_per,nutriscore_grade,nutriments," +
                "image_front_small_url,image_front_url"
    }
}

internal class SearchAliciousException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
