package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket.CloudFlareException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.ws.rs.NotFoundException
import org.springframework.http.HttpStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

sealed class WorkerStrategy(
    override val id: String,
    override val displayName: String,
    private val workerUrl: String,
) : ScrapingStrategy {
    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient.newBuilder().build()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override val isAvailable: Boolean get() = workerUrl.isNotBlank()

    override suspend fun fetch(url: String): String {
        logger.debug { "Fetch via worker ($id): $url" }
        val requestBody = """{"url":"${url.replace("\"", "\\\"")}"}"""
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create("$workerUrl/fetch"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            logger.warn { "Worker ($id) returned ${response.statusCode()}: ${response.body().take(200)}" }
            throw RuntimeException("Scraper worker ($id) returned ${response.statusCode()}")
        }
        val workerResponse = objectMapper.readValue(response.body(), WorkerResponse::class.java)
        if (workerResponse.status == 403) {
            throw CloudFlareException(HttpStatusCode.valueOf(403))
        }
        if (workerResponse.status == 404) {
            throw NotFoundException(url)
        }
        logger.debug { "Worker ($id) returned status ${workerResponse.status}, content length: ${workerResponse.content?.length}" }
        return workerResponse.content ?: throw RuntimeException("Worker ($id) returned no content")
    }

    override fun close() {}

    private data class WorkerResponse(
        val status: Int,
        val content: String? = null,
        val url: String? = null,
        val error: String? = null,
    )
}
