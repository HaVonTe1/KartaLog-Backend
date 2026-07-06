package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Playwright
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.BrowserContextPool
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket.CloudFlareException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatusCode
import kotlin.random.Random

class ChromeCdpStrategy(
    private val cdpUrl: String,
    private val poolSize: Int = 1,
    private val maxConcurrent: Int = 1,
) : ScrapingStrategy {
    private val logger = KotlinLogging.logger {}

    override val id: String = "chrome-cdp"
    override val displayName: String = "Real Chrome via CDP"
    override val isAvailable: Boolean = cdpUrl.isNotBlank()

    private var playwrightRef: Playwright? = null
    private var browserRef: Browser? = null
    private var contextPoolRef: BrowserContextPool? = null

    private fun ensureInitialized() {
        if (playwrightRef == null) {
            playwrightRef = Playwright.create()
            browserRef = playwrightRef!!.chromium().connectOverCDP(cdpUrl)
            contextPoolRef =
                BrowserContextPool(browserRef!!, poolSize, maxConcurrent) { br ->
                    val ctx = br.contexts().firstOrNull() ?: br.newContext()
                    ctx
                }
            logger.info { "Chrome CDP strategy initialized (connected to $cdpUrl)" }
        }
    }

    override suspend fun fetch(url: String): String {
        ensureInitialized()
        val pool = contextPoolRef!!
        return pool.use { context ->
            val page = context.newPage()
            try {
                randomDelay()
                try {
                    page.navigate(url, com.microsoft.playwright.Page.NavigateOptions().setTimeout(60000.0))
                } catch (_: com.microsoft.playwright.TimeoutError) {
                    logger.info { "Navigation timed out, starting poll..." }
                }
                val deadline = System.currentTimeMillis() + 90000L
                var detected = false
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val html = page.evaluate("() => document.documentElement.outerHTML") as String
                        if (html.contains("ProductSearchInput") || html.contains("CardmarketNewsLink")) {
                            detected = true
                            break
                        }
                        if (!html.contains("cf_chl_opt") && !html.contains("/cdn-cgi/challenge-platform/")
                            && !html.contains("Just a moment") && !html.contains("Attention Required")
                            && !html.contains("Nur einen Moment") && !html.contains("Un instant")) {
                            detected = true
                            break
                        }
                    } catch (_: Exception) {
                        logger.info { "page.evaluate failed, retrying..." }
                    }
                    Thread.sleep(2000)
                }
                if (detected) {
                    logger.info { "CardMarket content detected" }
                } else {
                    logger.warn { "Cloudflare challenge not resolved within timeout" }
                    throw CloudFlareException(HttpStatusCode.valueOf(503))
                }
                page.content()
            } finally {
                page.close()
            }
        }
    }

    private fun randomDelay() {
        Thread.sleep(Random.nextLong(1000L, 3000L))
    }

    override fun close() {
        logger.info { "Closing Chrome CDP strategy..." }
        try {
            contextPoolRef?.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
        browserRef?.close()
        playwrightRef?.close()
    }
}
