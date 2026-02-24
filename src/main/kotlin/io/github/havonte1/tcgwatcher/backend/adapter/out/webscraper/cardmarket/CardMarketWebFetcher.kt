package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.PlaywrightManager
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import jakarta.ws.rs.NotFoundException
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.log

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

    @Retry(name = "cardMarketRetry")
    @CircuitBreaker(name = "cardMarketCircuitBreaker")
    @RateLimiter(name = "cardMarketRateLimiter")
    open override fun fetch(searchString: String, locale: String, game: String): Result<String> {
        logger.debug { "Fetching search results  from $searchString" }
        return Result.success(performFetch(searchString, locale, game))
    }

    @Retry(name = "cardMarketRetry")
    @CircuitBreaker(name = "cardMarketCircuitBreaker")
    @RateLimiter(name = "cardMarketRateLimiter")
    open override fun fetchDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): Result<String> {
        return Result.success(performFetchDetails(cmId, genre, type, lang, setname))
    }

    private fun createContextOptions(): Browser.NewContextOptions {
        return Browser.NewContextOptions()
            .setGeolocation(BERLIN_LAT, BERLIN_LONG)
            .setPermissions(listOf("geolocation"))
            .setUserAgent(USERAGENT)
    }

    private fun fetchUrl(url: String): String {
        val browser: Browser = playwrightManager.browser
        val contextOptions = createContextOptions()
        val storageFile = Path.of("auth.json")
        if (Files.exists(storageFile)) {
            contextOptions.setStorageStatePath(storageFile)
        }
        val context = browser.newContext(contextOptions)
        val page: Page = context.newPage()
        logger.debug { "Naviage to $url" }
        val response = page.navigate(url, Page.NavigateOptions().setTimeout(10000.0))
        logger.debug { "Response: ${response.status()}" }
        page.waitForLoadState(LoadState.DOMCONTENTLOADED)
        if (!response.ok()) {
            when(response.status()) {
                404 -> {
                    throw NotFoundException(response.url())
                }
                403 -> {
                    throw CloudFlareException(HttpStatusCode.valueOf(response.status()))
                }
            }
        }
        val content = page.content()
        logger.debug { "Fetched content length: ${content.length}" }
        context.storageState(BrowserContext.StorageStateOptions().setPath(Path.of("auth.json")))
        context.close()
        return content
    }

    private fun performFetch(searchString: String, locale: String, game: String): String {
        logger.debug { "performFetch" }
        val encodedSearchString = URLEncoder.encode(searchString, Charsets.UTF_8)
        val url = buildUrl(locale, game, encodedSearchString)
        return fetchUrl(url)
    }

    private fun performFetchDetails(
        cmId: String,
        genre: String,
        type: String,
        lang: String,
        setname: String
    ): String {
        val detailsUrl = buildDetailUrl(lang, genre, type, setname, cmId)
        return fetchUrl(detailsUrl)
    }

    private fun buildUrl(locale: String, game: String, encodedSearchString: String): String {
        val finalLocale = locale.ifEmpty { CardMarketConstants.DEFAULT_LOCALE }
        val finalGame = game.ifEmpty { CardMarketConstants.DEFAULT_GAME }
        return "${config.basePath}${CardMarketConstants.PATH_SEPARATOR}$finalLocale${CardMarketConstants.PATH_SEPARATOR}$finalGame/Products/Search?searchString=$encodedSearchString"
    }

    private fun buildDetailUrl(lang: String, genre: String, type: String, setname: String, cmId: String): String {
        val finalLocale = lang.ifEmpty { CardMarketConstants.DEFAULT_LOCALE }
        val finalGame = genre.ifEmpty { CardMarketConstants.DEFAULT_GAME }
        return "${config.basePath}/$finalLocale/$finalGame/Products/$type/$setname/$cmId"
    }
}
