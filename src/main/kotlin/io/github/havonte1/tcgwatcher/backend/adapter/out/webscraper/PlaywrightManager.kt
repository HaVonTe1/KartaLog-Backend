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
    @Value("\${playwright.executable-path:}") private val executablePath: String?,
) {
    private val logger = KotlinLogging.logger {}
    private val options = BrowserType.LaunchOptions().apply {
        setHeadless(true)
        if (!this@PlaywrightManager.executablePath.isNullOrBlank()) {
            setExecutablePath(Paths.get(this@PlaywrightManager.executablePath))
        }
        setArgs(
            listOf(
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
            )
        )
    }

    private var playwrightInstance: Playwright? = null
    private var browserInstance: Browser? = null
    private var initialized = false

    val playwright: Playwright
        get() {
            if (playwrightInstance == null) {
                playwrightInstance = Playwright.create()
            }
            return playwrightInstance!!
        }

    val browser: Browser
        get() {
            if (browserInstance == null) {
                browserInstance = playwright.chromium().launch(options)
                initialized = true
            }
            return browserInstance!!
        }

    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down Playwright..." }
        if (initialized) {
            try {
                browserInstance?.close()
            } catch (e: Exception) {
                logger.warn { e.message }
            }
        }
        try {
            playwrightInstance?.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
    }
}
