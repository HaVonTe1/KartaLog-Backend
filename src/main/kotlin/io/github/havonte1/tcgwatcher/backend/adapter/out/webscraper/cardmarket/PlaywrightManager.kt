package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.BrowserType.LaunchOptions
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

/**
 * Manages a singleton Playwright instance and a shared Chromium browser.
 * The browser is created once and reused across requests to avoid the heavy
 * cost of launching a new process per fetch. Contexts are created per request
 * and closed after use.
 */
@Component
class PlaywrightManager {
    val playwright: Playwright = Playwright.create()
    val browser: Browser = playwright.chromium().launch(
        LaunchOptions().setHeadless(true)
    )

    @PreDestroy
    fun shutdown() {
        // Close the browser and the underlying Playwright instance when the
        // application context is destroyed.
        try {
            browser.close()
        } catch (e: Exception) {
            // Swallow any exception during shutdown – the container is stopping.
        }
        try {
            playwright.close()
        } catch (e: Exception) {
            // Same rationale as above.
        }
    }
}
