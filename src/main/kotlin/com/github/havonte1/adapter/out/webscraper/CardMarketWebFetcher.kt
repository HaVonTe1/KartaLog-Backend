package com.github.havonte1.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Responsible for performing the HTTP fetch using Playwright.
 * Returns the raw HTML content of the CardMarket search page.
 */

class CardMarketWebFetcher {
    private val logger = KotlinLogging.logger {}

    /**
     * Fetches the search page HTML for the given [searchString].
     * The function creates a Playwright instance, launches a Chromium browser,
     * navigates to the search URL, waits for network idle, and returns the page content.
     */
    fun fetch(searchString: String): String {
        logger.info { "Fetching CardMarket page for \"$searchString\"" }
        val playwright = Playwright.create()
        playwright.use { playwright ->
            val browser: Browser = playwright.chromium().launch(
                LaunchOptions()
                    .setHeadless(true)

            )
            val contextOptions = Browser.NewContextOptions()
                .setGeolocation(52.5200, 13.4050) // Berlin: Lat, Long
                .setPermissions(listOf("geolocation")) // Berechtigung automatisch erteilen
                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")

            val storageFile = Paths.get("auth.json")

            if(storageFile.exists()) {
                contextOptions.setStorageStatePath(storageFile)

            }
            val context = browser.newContext(contextOptions)
            val page: Page = context.newPage()
            val url = "https://www.cardmarket.com/de/Pokemon/Products/Search?searchString=$searchString"
            page.navigate(url)
            logger.debug { "Navigated to ${page.url()}" }
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)


            val content = page.content()
            logger.debug { "Fetched content length: ${content.length}" }

            // Zustand (Cookies & LocalStorage) in Datei speichern
            context.storageState(
                BrowserContext.StorageStateOptions()
                .setPath(Paths.get("auth.json")))
            browser.close()
            return content
        }
    }
}
