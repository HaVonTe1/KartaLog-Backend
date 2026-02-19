package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component

/**
 * Manages a singleton Playwright instance and a shared Chromium browser.
 * The browser is created once and reused across requests to avoid the heavy
 * cost of launching a new process per fetch. Contexts are created per request
 * and closed after use.
 */
@Component
class PlaywrightManager {
    private val logger = KotlinLogging.logger {}

    val playwright: Playwright = Playwright.create()
    val browser: Browser = playwright.chromium().launch(
        BrowserType.LaunchOptions().setHeadless(true)
    )

    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down Playwright..." }
        // Close the browser and the underlying Playwright instance when the
        // application context is destroyed.
        try {
            browser.close()
        } catch (e: Exception) {
            logger.warn { e.message }
            // Swallow any exception during shutdown – the container is stopping.
        }
        try {
            playwright.close()
        } catch (e: Exception) {
            logger.warn { e.message }
            // Same rationale as above.
        }
    }
}
