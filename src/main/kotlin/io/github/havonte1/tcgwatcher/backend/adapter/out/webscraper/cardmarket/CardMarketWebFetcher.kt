package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.file.Paths
import java.time.Duration
import java.util.function.Supplier
import kotlin.io.path.exists

private const val USERAGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"

private const val BERLIN_LAT = 52.5200

private const val BERLIN_LONG = 13.4050

@Component
class CardMarketWebFetcher(
    private val playwrightManager: PlaywrightManager = PlaywrightManager(),
    private val config: CardMarketConfig = CardMarketConfig()
) : CardMarketWebFetcherPort {
    private val logger = KotlinLogging.logger {}

    internal val circuitBreaker: CircuitBreaker by lazy {
        createCircuitBreaker()
    }

    internal val retry: Retry by lazy {
        createRetry()
    }

    private fun createCircuitBreaker(): CircuitBreaker {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(config.circuitBreaker.failureThreshold.toFloat())
            .slowCallRateThreshold(100.0F)
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .minimumNumberOfCalls(10)
            .slidingWindowSize(config.circuitBreaker.slidingWindowSec.toInt())
            .waitDurationInOpenState(Duration.ofSeconds(config.circuitBreaker.waitDurationSec))
            .build()
        return CircuitBreaker.of("cardMarketFetcherCB", circuitBreakerConfig)
    }

    private fun createRetry(): Retry {
        val retryConfig = RetryConfig.custom<String>()
            .maxAttempts(config.retryAttempts)
            .waitDuration(Duration.ofMillis(500))

            .build()
        return Retry.of("cardMarketFetcherRetry", retryConfig)
    }

    override fun fetch(searchString: String, locale: String, game: String): Result<String> {
        val supplier = Supplier<String> { performFetch(searchString, locale, game) }

        val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier))

        return try {
            Result.success(decoratedSupplier.get())
        } catch (e: Exception) {
            logger.warn { "Failed to fetch CardMarket page: ${e.message}" }
            Result.failure(e)
        }
    }

    override fun fetchDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): Result<String> {
        val supplier = Supplier<String> {
            performFetchDetails(
                cmId = cmId,
                genre = genre,
                type = type,
                lang = lang,
                setname = setname
            )
        }

        val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier))

        return try {
            Result.success(decoratedSupplier.get())
        } catch (e: Exception) {
            logger.warn { "Failed to fetch CardMarket detail page: ${e.message}" }
            Result.failure(e)
        }
    }

    private fun performFetch(searchString: String, locale: String, game: String): String =
        playwrightManager.playwright.use {
            val browser: Browser = playwrightManager.browser
            val contextOptions = Browser.NewContextOptions()
                .setGeolocation(BERLIN_LAT, BERLIN_LONG)
                .setPermissions(listOf("geolocation"))
                .setUserAgent(USERAGENT)

            val storageFile = Paths.get("auth.json")
            if (storageFile.exists()) {
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
            context.storageState(BrowserContext.StorageStateOptions().setPath(Paths.get("auth.json")))
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

        val storageFile = Paths.get("auth.json")
        if (storageFile.exists()) {
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
        context.storageState(BrowserContext.StorageStateOptions().setPath(Paths.get("auth.json")))
        context.close()
        content
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
