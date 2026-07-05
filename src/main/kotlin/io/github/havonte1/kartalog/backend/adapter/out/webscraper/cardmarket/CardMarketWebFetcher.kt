package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.PlaywrightManager
import io.github.havonte1.kartalog.backend.config.CardMarketConfig
import io.github.havonte1.kartalog.backend.config.GenreConfig
import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import jakarta.ws.rs.NotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
open class CardMarketWebFetcher(
    private val playwrightManager: PlaywrightManager,
    private val config: CardMarketConfig,
    @Value("\${scraper.worker.url:}") private val workerUrl: String,
) : CardMarketWebFetcherPort {
    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient.newBuilder().build()
    private val objectMapper = ObjectMapper().registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())

    private val useWorker get() = workerUrl.isNotBlank()

    @Retry(name = "cardMarketRetry")
    @CircuitBreaker(name = "cardMarketCircuitBreaker")
    override suspend fun fetch(
        searchString: String,
        locale: Locale,
        genre: Genre,
        page: Int
    ): Result<String> {
        logger.debug { "Fetching search results from $searchString" }
        return Result.success(performFetch(searchString, locale, genre, page))
    }

    @Retry(name = "cardMarketRetry")
    @CircuitBreaker(name = "cardMarketCircuitBreaker")
    override suspend fun fetchDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): Result<String> = Result.success(performFetchDetails(cmId, genre, type, locale, setname))

    private suspend fun fetchUrl(url: String): String {
        if (useWorker) {
            return fetchViaWorker(url)
        }
        return fetchViaPlaywright(url)
    }

    private suspend fun fetchViaWorker(url: String): String {
        logger.debug { "Fetch via worker: $url" }
        val requestBody = """{"url":"${url.replace("\"", "\\\"")}"}"""
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create("$workerUrl/fetch"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            logger.warn { "Worker returned ${response.statusCode()}: ${response.body().take(200)}" }
            throw RuntimeException("Scraper worker returned ${response.statusCode()}")
        }
        val workerResponse = objectMapper.readValue(response.body(), WorkerResponse::class.java)
        if (workerResponse.status == 403) {
            throw CloudFlareException(HttpStatusCode.valueOf(403))
        }
        if (workerResponse.status == 404) {
            throw NotFoundException(url)
        }
        logger.debug { "Worker returned status ${workerResponse.status}, content length: ${workerResponse.content?.length}" }
        return workerResponse.content ?: throw RuntimeException("Worker returned no content")
    }

    private data class WorkerResponse(
        val status: Int,
        val content: String? = null,
        val url: String? = null,
        val error: String? = null,
    )

    private suspend fun fetchViaPlaywright(url: String): String {
        val contextPool = playwrightManager.getContextPool()
        val content = contextPool.use { context ->
            val page: Page = context.newPage()
            page.setExtraHTTPHeaders(
                mapOf(
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                    "Accept-Language" to "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Sec-Fetch-Dest" to "document",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "none",
                    "Sec-Fetch-User" to "?1",
                    "Upgrade-Insecure-Requests" to "1",
                )
            )
            playwrightManager.randomDelay()
            logger.debug { "Navigate to $url" }
            val response = page.navigate(url, Page.NavigateOptions().setTimeout(20000.0))
            logger.debug { "Response: ${response.status()}" }
            page.waitForLoadState(LoadState.NETWORKIDLE)
            if (!response.ok()) {
                when (response.status()) {
                    404 -> throw NotFoundException(response.url())
                    403 -> throw CloudFlareException(HttpStatusCode.valueOf(response.status()))
                }
            }
            val fetchedContent = page.content()
            logger.debug { "Fetched content length: ${fetchedContent.length}" }
            fetchedContent
        }
        return content
    }

    private suspend fun performFetch(
        searchString: String,
        locale: Locale,
        genre: Genre,
        page: Int,
    ): String {
        val encodedSearchString = URLEncoder.encode(searchString, Charsets.UTF_8)
        val url = buildUrl(locale, genre, encodedSearchString, page)
        return fetchUrl(url)
    }

    // this method needs all params supported by the cardmarket search engine
    private fun buildUrl(
        locale: Locale,
        genre: Genre,
        encodedSearchString: String,
        page: Int,
    ): String {
        val genreConfigData = GenreConfig.GENRES[genre]
        assert(genreConfigData != null)
        val pathPattern = genreConfigData!!.searchPathPattern
        val path = String.format(pathPattern, locale.code)
        val basePath = config.basePath
        val searchParams = CardMarketSearchParams.combineAll(encodedSearchString,  page)
        val queryString = searchParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$basePath$path/Products/Search?$queryString"
    }

    private suspend fun performFetchDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): String {
        val detailsUrl = buildDetailUrl(locale, genre, type, setname, cmId)
        return fetchUrl(detailsUrl)
    }



    private fun buildDetailUrl(
        locale: Locale,
        genre: Genre,
        type: ProductType,
        setname: String,
        cmId: String,
    ): String {
        val detailsUrlBase = GenreConfig.buildDetailsUrlBase(genre, locale, type)
        val basePath = config.basePath
        return "$basePath$detailsUrlBase/${URLEncoder.encode(setname, Charsets.UTF_8)}/$cmId"
    }
}
