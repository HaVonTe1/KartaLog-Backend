package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.stereotype.Component
import java.nio.file.Paths
import kotlin.io.path.exists

import java.time.Duration
import java.util.function.Supplier

private const val USERAGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"

private const val BERLIN_LAT = 52.5200

private const val BERLIN_LONG = 13.4050

/**
 * Responsible for performing the HTTP fetch using Playwright.
 * Returns the raw HTML content of the CardMarket search page.
 */

@Component
class CardMarketWebFetcher(
    private val playwrightManager: PlaywrightManager = PlaywrightManager(),
    private val config: CardMarketConfig = CardMarketConfig()
) : CardMarketWebFetcherPort {
    private val logger = KotlinLogging.logger {}

    /**
     * Fetches the search page HTML for the given [searchString].
     * The function creates a Playwright instance, launches a Chromium browser,
     * navigates to the search URL, waits for network idle, and returns the page content.
     */

    override fun fetch(searchString: String, locale: String, game: String): Result<String> {
        // Build Resilience4j Retry and CircuitBreaker based on config
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(config.retryAttempts)
            .waitDuration(Duration.ofMillis(500))
            .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(500, 2.0))
            .build()
        val retry = Retry.of("cardMarketFetcherRetry", retryConfig)

        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0F)
            .slowCallRateThreshold(100.0F)
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .minimumNumberOfCalls(config.circuitBreaker.failureThreshold)
            .slidingWindowSize(config.circuitBreaker.slidingWindowSec.toInt())
            .waitDurationInOpenState(Duration.ofSeconds(config.circuitBreaker.waitDurationSec))
            .build()
        val circuitBreaker = CircuitBreaker.of("cardMarketFetcherCB", circuitBreakerConfig)

        // Supplier that performs the actual Playwright fetch and returns Result
        val supplier = Supplier<Result<String>> {
            try {
                val playwright = playwrightManager.playwright
                playwright.use {
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
                    val url = "${config.basePath}/$locale/$game/Products/Search?searchString=$searchString"
                    // Apply timeout from config (milliseconds)
                    page.navigate(url, Page.NavigateOptions().setTimeout(config.timeoutMs.toDouble()))
                    logger.debug { "Navigated to ${page.url()}" }
                    page.waitForLoadState(LoadState.DOMCONTENTLOADED)
                    val content = page.content()
                    logger.debug { "Fetched content length: ${content.length}" }
                    // Save storage state
                    context.storageState(BrowserContext.StorageStateOptions().setPath(Paths.get("auth.json")))
                    browser.close()
                    Result.success(content)
                }
            } catch (e: Exception) {
                logger.warn { "Failed to fetch CardMarket page: ${e.message}" }
                Result.failure(e)
            }
        }

        // Apply retry then circuit breaker to the supplier
        val retrySupplier = Retry.decorateSupplier(retry, supplier)
        val cbSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retrySupplier)
        return cbSupplier.get()
    }
        // (Old implementation removed)
}
