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
    @Value("\${playwright.context-pool-size:3}") private val poolSize: Int,
    @Value("\${playwright.max-concurrent-requests:3}") private val maxConcurrent: Int,
) {
    private val logger = KotlinLogging.logger {}
    private val options =
        BrowserType.LaunchOptions().apply {
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
                    "--disable-background-networking",
                    "--disable-default-apps",
                    "--disable-extensions",
                    "--disable-sync",
                    "--disable-translate",
                    "--metrics-recording-only",
                    "--mute-audio",
                    "--headless=new",
                ),
            )
        }

    private companion object {
        private const val BERLIN_LAT = 52.5200
        private const val BERLIN_LONG = 13.4050
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
    }

    private var playwrightInstance: Playwright? = null
    private var browserInstance: Browser? = null
    private var contextPool: BrowserContextPool? = null
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

    fun getContextPool(): BrowserContextPool {
        if (contextPool == null) {
            contextPool = BrowserContextPool(browser, poolSize, maxConcurrent) { br ->
                br.newContext(createContextOptions())
            }
        }
        return contextPool!!
    }

    private fun createContextOptions(): Browser.NewContextOptions =
        Browser
            .NewContextOptions()
            .setGeolocation(BERLIN_LAT, BERLIN_LONG)
            .setPermissions(listOf("geolocation"))
            .setUserAgent(DEFAULT_USER_AGENT)

    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down Playwright..." }
        contextPool?.close()
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
