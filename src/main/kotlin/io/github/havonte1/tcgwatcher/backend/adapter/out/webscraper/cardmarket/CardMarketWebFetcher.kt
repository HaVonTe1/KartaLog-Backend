package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Paths
import kotlin.io.path.exists

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
    private val playwrightManager: PlaywrightManager = PlaywrightManager()
) : CardMarketWebFetcherPort {
    private val logger = KotlinLogging.logger {}
    val baseUrl = System.getenv("CARDMARKET_BASE_URL") ?: "https://www.cardmarket.com"

    /**
     * Fetches the search page HTML for the given [searchString].
     * The function creates a Playwright instance, launches a Chromium browser,
     * navigates to the search URL, waits for network idle, and returns the page content.
     */

    override fun fetch(searchString: String, locale: String, game: String): String {
        logger.debug { "Fetching CardMarket page for \"$searchString\"" }
        val playwright = playwrightManager.playwright
        playwright.use { _ ->
            val browser: Browser = playwrightManager.browser
            val contextOptions = Browser.NewContextOptions()
                .setGeolocation(BERLIN_LAT, BERLIN_LONG) // Berlin: Lat, Long
                .setPermissions(listOf("geolocation")) // Berechtigung automatisch erteilen
                .setUserAgent(
                    USERAGENT
                )

            val storageFile = Paths.get("auth.json")

            if (storageFile.exists()) {
                contextOptions.setStorageStatePath(storageFile)
            }
            val context = browser.newContext(contextOptions)
            val page: Page = context.newPage()
            val url = "$baseUrl/$locale/$game/Products/Search?searchString=$searchString"
            page.navigate(url)
            logger.debug { "Navigated to ${page.url()}" }
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)

            val content = page.content()
            logger.debug { "Fetched content length: ${content.length}" }

            // Zustand (Cookies & LocalStorage) in Datei speichern
            context.storageState(
                BrowserContext.StorageStateOptions()
                    .setPath(Paths.get("auth.json"))
            )
            browser.close()
            return content
        }
    }
}
