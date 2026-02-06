package com.github.havonte1.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

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
        try {
            val browser: Browser = playwright.chromium().launch(
                LaunchOptions()
                    .setHeadless(false) // headless false can bypass some Cloudflare checks
                    .setArgs(listOf("--disable-blink-features=AutomationControlled"))
            )
            val page: Page = browser.newPage()
            val url = "https://www.cardmarket.com/de/Pokemon/Products/Search?searchString=$searchString"
            page.navigate(url)
            logger.debug { "Navigated to ${page.url()}" }
            page.waitForLoadState(LoadState.NETWORKIDLE)


            val content = page.content()
            logger.debug { "Fetched content length: ${content.length}" }
            browser.close()
            return content
        } finally {
            playwright.close()
        }
    }
}
