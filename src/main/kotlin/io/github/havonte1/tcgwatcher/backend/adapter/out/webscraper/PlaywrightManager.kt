package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class PlaywrightManager(
    @Value("\${playwright.executable-path:/usr/bin/chromium}") private val executablePath: String,
) {
    private val logger = KotlinLogging.logger {}

    val playwright: Playwright = Playwright.create()
    val browser: Browser = playwright.chromium().launch(
        BrowserType.LaunchOptions()
            .setHeadless(true)
            .setExecutablePath(Paths.get(executablePath))
            .setArgs(listOf(
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-gpu",
                "--disable-dev-shm-usage",
                "--disable-software-rasterizer",
                "--disable-accelerated-2d-canvas",
                "--no-first-run",
                "--no-zygote",
                "--single-process",
                "--disable-background-networking",
                "--disable-default-apps",
                "--disable-extensions",
                "--disable-sync",
                "--disable-translate",
                "--metrics-recording-only",
                "--mute-audio",
                "--headless=new"
            ))
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
