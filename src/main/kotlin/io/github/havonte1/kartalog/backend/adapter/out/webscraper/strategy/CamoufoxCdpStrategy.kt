package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.BrowserContextPool
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket.CloudFlareException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.ws.rs.NotFoundException
import kotlinx.coroutines.delay
import org.springframework.http.HttpStatusCode
import java.net.URI
import java.util.WeakHashMap
import kotlin.random.Random

class CamoufoxCdpStrategy(
    private val wsUrl: String,
    private val healthUrl: String,
    private val poolSize: Int = 3,
    private val maxConcurrent: Int = 3,
) : ScrapingStrategy {
    private val logger = KotlinLogging.logger {}

    override val id: String = "camoufox-cdp"
    override val displayName: String = "Camoufox via CDP"
    override val isAvailable: Boolean = wsUrl.isNotBlank()

    private var playwrightRef: Playwright? = null
    private var browserRef: Browser? = null
    private var contextPoolRef: BrowserContextPool? = null
    private val warmedContexts = WeakHashMap<BrowserContext, Boolean>()

    @Synchronized
    private fun ensureInitialized() {
        if (contextPoolRef != null) return
        if (playwrightRef != null) {
            logger.warn { "Partial init detected, resetting..." }
            close()
        }
        playwrightRef = Playwright.create()
        try {
            val actualWsUrl = discoverWsEndpoint()
            logger.info { "Connecting to Camoufox at $actualWsUrl" }
            val browser = playwrightRef!!.firefox().connect(actualWsUrl)
            browserRef = browser
            val ctxOptions =
                Browser.NewContextOptions()
                    .setGeolocation(52.5200, 13.4050)
                    .setPermissions(listOf("geolocation"))
                    .setUserAgent(FIREFOX_USER_AGENT)
                    .setLocale("de-DE")
                    .setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT)
                    .setViewportSize(null as com.microsoft.playwright.options.ViewportSize?)
            contextPoolRef =
                BrowserContextPool(browser, poolSize, maxConcurrent) { br ->
                    val ctx = br.newContext(ctxOptions)
                    ctx.addInitScript(SHADOW_ROOT_PATCH)
                    ctx.addInitScript(STEALTH_SCRIPT)
                    ctx
                }
            logger.info { "Camoufox CDP strategy initialized (connected to $wsUrl), pool=$poolSize, maxConcurrent=$maxConcurrent" }
        } catch (e: Exception) {
            logger.error { "Failed to initialize Camoufox CDP strategy: ${e.message}" }
            close()
            throw e
        }
    }

    override suspend fun fetch(url: String): String {
        ensureInitialized()
        val pool = contextPoolRef!!
        return pool.use { context ->
            val page = context.newPage()
            try {
                page.setExtraHTTPHeaders(
                    mapOf(
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                        "Accept-Language" to "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7",
                        "Sec-Fetch-Dest" to "document",
                        "Sec-Fetch-Mode" to "navigate",
                        "Sec-Fetch-Site" to "none",
                        "Sec-Fetch-User" to "?1",
                        "Upgrade-Insecure-Requests" to "1",
                    )
                )
                warmupSession(context, page)
                randomDelay()
                val response = page.navigate(url, com.microsoft.playwright.Page.NavigateOptions().setTimeout(20000.0))
                page.waitForLoadState(LoadState.NETWORKIDLE)
                if (response.status() == 404) throw NotFoundException(response.url())
                if (!response.ok() || CloudflareChallengeSolver.hasChallenge(page)) {
                    if (CloudflareChallengeSolver.trySolve(page)) {
                        page.waitForLoadState(LoadState.NETWORKIDLE)
                    } else {
                        throw CloudFlareException(HttpStatusCode.valueOf(response.status()))
                    }
                }
                page.content()
            } finally {
                page.close()
            }
        }
    }

    private fun discoverWsEndpoint(): String {
        val healthUri = URI(healthUrl)
        val healthResponse = healthUri.toURL().readText()
        val wsKey = "\"wsEndpoint\":\""
        val start = healthResponse.indexOf(wsKey)
        if (start == -1) {
            throw RuntimeException("Failed to find wsEndpoint in health response: $healthResponse")
        }
        val wsStart = start + wsKey.length
        val wsEnd = healthResponse.indexOf('"', wsStart)
        val discovered = healthResponse.substring(wsStart, wsEnd)

        val configuredUri = URI(wsUrl)
        val discoveredUri = URI(discovered)
        val discoveredPort = discoveredUri.port.takeIf { it != -1 } ?: 9226
        val configuredPort = configuredUri.port.takeIf { it != -1 } ?: 9225
        return "ws://${configuredUri.host}:$configuredPort${discoveredUri.path}"
    }

    private suspend fun warmupSession(context: BrowserContext, page: com.microsoft.playwright.Page) {
        if (warmedContexts[context] == true) return
        try {
            page.navigate("https://www.cardmarket.com", com.microsoft.playwright.Page.NavigateOptions().setTimeout(15000.0))
            page.waitForLoadState(LoadState.NETWORKIDLE)
            warmedContexts[context] = true
        } catch (_: Exception) {
        }
    }

    private suspend fun randomDelay() {
        delay(Random.nextLong(1000L, 3000L))
    }

    override fun close() {
        logger.info { "Closing Camoufox CDP strategy..." }
        try {
            contextPoolRef?.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
        try {
            browserRef?.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
        try {
            playwrightRef?.close()
        } catch (e: Exception) {
            logger.warn { e.message }
        }
    }

    private companion object {
        private const val FIREFOX_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"

        private val SHADOW_ROOT_PATCH = """
(() => {
    if (window._shadowRootPatched) return;
    window._shadowRootPatched = true;
    const roots = new WeakMap();
    const orig = Element.prototype.attachShadow;
    Element.prototype.attachShadow = function(init) {
        const sr = orig.call(this, {...init, mode: 'open'});
        roots.set(this, sr);
        return sr;
    };
    const desc = Object.getOwnPropertyDescriptor(Element.prototype, 'shadowRoot');
    if (desc && desc.get) {
        const getter = desc.get;
        Object.defineProperty(Element.prototype, 'shadowRoot', {
            get() { return getter.call(this) || roots.get(this); },
            configurable: true
        });
    }
})();
""".trimIndent()

        private val STEALTH_SCRIPT = """
Object.defineProperty(navigator, 'webdriver', { get: () => false });

Object.defineProperty(navigator, 'plugins', {
    get: () => [
        { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer' },
        { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai' },
        { name: 'Native Client', filename: 'internal-nacl-plugin' }
    ]
});

Object.defineProperty(navigator, 'languages', {
    get: () => ['de-DE', 'de', 'en-US', 'en']
});

Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });

if (typeof window.chrome === 'undefined') {
    window.chrome = { runtime: {} };
}

if (navigator.permissions && navigator.permissions.query) {
    const originalQuery = navigator.permissions.query.bind(navigator.permissions);
    navigator.permissions.query = (params) => {
        if (params.name === 'notifications') {
            return Promise.resolve({ state: 'denied', onchange: null });
        }
        return originalQuery(params);
    };
}

Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 0 });
""".trimIndent()
    }
}
