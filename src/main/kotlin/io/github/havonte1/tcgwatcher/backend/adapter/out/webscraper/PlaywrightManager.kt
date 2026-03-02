package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class PlaywrightManager {
    private val logger = KotlinLogging.logger {}

    init {
        System.setProperty("playwright.browser.validate.hostDependencies", "false")
    }

    private val executablePath: String = System.getProperty("playwright.chromium.executablePath", "/usr/bin/chromium")

    val playwright: Playwright = Playwright.create()
    val browser: Browser = playwright.chromium().launch(
        BrowserType.LaunchOptions()
            .setHeadless(true)
            .setExecutablePath(Paths.get(executablePath))
    )

    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down Playwright..." }
        try {
            browser.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
        try {
            playwright.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
    }
}
