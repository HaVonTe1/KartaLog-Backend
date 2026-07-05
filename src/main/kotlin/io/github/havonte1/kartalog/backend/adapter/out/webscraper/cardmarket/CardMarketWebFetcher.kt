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
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
open class CardMarketWebFetcher(
    private val playwrightManager: PlaywrightManager,
    private val config: CardMarketConfig,
) : CardMarketWebFetcherPort {
    private val logger = KotlinLogging.logger {}

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
        val contextPool = playwrightManager.getContextPool()
        val content = contextPool.use { context ->
            val page: Page = context.newPage()
            logger.debug { "Navigate to $url" }
            val response = page.navigate(url, Page.NavigateOptions().setTimeout(20000.0))
            logger.debug { "Response: ${response.status()}" }
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)
            if (!response.ok()) {
                when (response.status()) {
                    404 -> {
                        throw NotFoundException(response.url())
                    }

                    403 -> {
                        throw CloudFlareException(HttpStatusCode.valueOf(response.status()))
                    }
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
