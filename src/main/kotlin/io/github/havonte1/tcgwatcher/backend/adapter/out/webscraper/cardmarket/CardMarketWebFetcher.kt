package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path

private const val USERAGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
private const val BERLIN_LAT = 52.5200
private const val BERLIN_LONG = 13.4050

@Component
open class CardMarketWebFetcher(
    private val playwrightManager: PlaywrightManager = PlaywrightManager(),
    private val config: CardMarketConfig = CardMarketConfig()
) : CardMarketWebFetcherPort {
    private val logger = KotlinLogging.logger {}

    @Retry(name = "cardMarketRetry", fallbackMethod = "retryFallback")
    @CircuitBreaker(name = "cardMarketCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
    override fun fetch(searchString: String, locale: String, game: String): Result<String> {
        return Result.success(performFetch(searchString, locale, game))
    }

    @Retry(name = "cardMarketRetry", fallbackMethod = "retryFallback")
    @CircuitBreaker(name = "cardMarketCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
    override fun fetchDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): Result<String> {
        return Result.success(performFetchDetails(cmId, genre, type, lang, setname))
    }

    private fun performFetch(searchString: String, locale: String, game: String): String =
        playwrightManager.playwright.use {
            val browser: Browser = playwrightManager.browser
            val contextOptions = Browser.NewContextOptions()
                .setGeolocation(BERLIN_LAT, BERLIN_LONG)
                .setPermissions(listOf("geolocation"))
                .setUserAgent(USERAGENT)

            val storageFile = Path.of("auth.json")
            if (Files.exists(storageFile)) {
                contextOptions.setStorageStatePath(storageFile)
            }
            val context = browser.newContext(contextOptions)
            val page: Page = context.newPage()
            val encodedSearchString = URLEncoder.encode(searchString, Charsets.UTF_8)
            val url = buildUrl(locale, game, encodedSearchString)
            page.navigate(url, Page.NavigateOptions().setTimeout(config.timeoutMs.toDouble()))
            logger.debug { "Navigated to ${page.url()}" }
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)
            val content = page.content()
            logger.debug { "Fetched content length: ${content.length}" }
            context.storageState(BrowserContext.StorageStateOptions().setPath(Path.of("auth.json")))
            context.close()
            content
        }

    private fun performFetchDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): String = playwrightManager.playwright.use {
        val browser: Browser = playwrightManager.browser
        val contextOptions = Browser.NewContextOptions()
            .setGeolocation(BERLIN_LAT, BERLIN_LONG)
            .setPermissions(listOf("geolocation"))
            .setUserAgent(USERAGENT)

        val storageFile = Path.of("auth.json")
        if (Files.exists(storageFile)) {
            contextOptions.setStorageStatePath(storageFile)
        }
        val context = browser.newContext(contextOptions)
        val page: Page = context.newPage()
        val detailsUrl = buildDetailUrl(lang, genre, type, setname, cmId)

        page.navigate(detailsUrl, Page.NavigateOptions().setTimeout(config.timeoutMs.toDouble()))
        logger.debug { "Navigated to ${page.url()}" }
        page.waitForLoadState(LoadState.DOMCONTENTLOADED)
        val content = page.content()
        logger.debug { "Fetched content length: ${content.length}" }
        context.storageState(BrowserContext.StorageStateOptions().setPath(Path.of("auth.json")))
        context.close()
        content
    }

    open fun retryFallback(
        searchString: String,
        locale: String,
        game: String,
        e: Exception
    ): Result<String> {
        logger.error { "All retry attempts exhausted for fetch(searchString=$searchString, locale=$locale, game=$game): ${e.message}" }
        return Result.failure(CircuitBreakerException("Retry limit exceeded for fetch", e))
    }

    open fun circuitBreakerFallback(
        searchString: String,
        locale: String,
        game: String,
        e: Exception
    ): Result<String> {
        logger.error { "Circuit breaker is OPEN, returning fallback: ${e.message}" }
        return Result.failure(CircuitBreakerException("Service unavailable due to circuit breaker state", e))
    }

    open fun retryFallback(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String,
        e: Exception
    ): Result<String> {
        logger.error { "All retry attempts exhausted for fetchDetails(cmId=$cmId): ${e.message}" }
        return Result.failure(CircuitBreakerException("Retry limit exceeded for fetchDetails", e))
    }

    open fun circuitBreakerFallback(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String,
        e: Exception
    ): Result<String> {
        logger.error { "Circuit breaker is OPEN for fetchDetails, returning fallback: ${e.message}" }
        return Result.failure(CircuitBreakerException("Service unavailable due to circuit breaker state", e))
    }

    private fun buildUrl(locale: String, game: String, encodedSearchString: String): String {
        val finalLocale = if (locale.isEmpty()) CardMarketConstants.DEFAULT_LOCALE else locale
        val finalGame = if (game.isEmpty()) CardMarketConstants.DEFAULT_GAME else game
        return "${config.basePath}${CardMarketConstants.PATH_SEPARATOR}$finalLocale${CardMarketConstants.PATH_SEPARATOR}$finalGame/Products/Search?searchString=$encodedSearchString"
    }

    private fun buildDetailUrl(lang: String, genre: String, type: String, setname: String, cmId: String): String {
        val finalLocale = if (lang.isEmpty()) CardMarketConstants.DEFAULT_LOCALE else lang
        val finalGame = if (genre.isEmpty()) CardMarketConstants.DEFAULT_GAME else genre
        return "${config.basePath}/$finalLocale/$finalGame/products/$type/$setname/$cmId"
    }
}

sealed class CardMarketFailure(
    message: String,
    cause: Throwable
) : Exception(message, cause)

class CircuitBreakerException(message: String, cause: Throwable) : CardMarketFailure(message, cause)
