package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.ScrapingStrategySelector
import io.github.havonte1.kartalog.backend.config.CardMarketConfig
import io.github.havonte1.kartalog.backend.config.GenreConfig
import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
open class CardMarketWebFetcher(
    private val selector: ScrapingStrategySelector,
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
        logger.debug { "Fetch via strategy '${selector.getActiveId()}': $url" }
        return selector.get().fetch(url)
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
