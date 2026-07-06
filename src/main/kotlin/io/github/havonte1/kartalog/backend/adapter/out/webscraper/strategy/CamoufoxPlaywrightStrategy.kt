package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.BrowserContextPool
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket.CloudFlareException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.ws.rs.NotFoundException
import org.springframework.http.HttpStatusCode
import kotlin.random.Random

class CamoufoxPlaywrightStrategy(
    private val executablePath: String? = null,
    private val poolSize: Int = 3,
    private val maxConcurrent: Int = 3,
) : ScrapingStrategy {
    private val logger = KotlinLogging.logger {}

    override val id: String = "camoufox"
    override val displayName: String = "Java Playwright - Camoufox"
    override val isAvailable: Boolean = true

    private var playwrightRef: Playwright? = null
    private var browserRef: Browser? = null
    private var contextPoolRef: BrowserContextPool? = null

    private fun ensureInitialized() {
        if (playwrightRef == null) {
            playwrightRef = Playwright.create()
            val path = executablePath.takeIf { !it.isNullOrBlank() } ?: "/opt/camoufox/camoufox-bin"
            val options =
                BrowserType.LaunchOptions().apply {
                    setHeadless(true)
                    setExecutablePath(java.nio.file.Paths.get(path))
                }
            browserRef = playwrightRef!!.firefox().launch(options)
            val ctxOptions =
                Browser.NewContextOptions()
                    .setGeolocation(52.5200, 13.4050)
                    .setPermissions(listOf("geolocation"))
                    .setUserAgent(FIREFOX_USER_AGENT)
                    .setLocale("de-DE")
                    .setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT)
                    .setViewportSize(null as com.microsoft.playwright.options.ViewportSize?)
            contextPoolRef =
                BrowserContextPool(browserRef!!, poolSize, maxConcurrent) { br ->
                    val ctx = br.newContext(ctxOptions)
                    ctx.addInitScript(SHADOW_ROOT_PATCH)
                    ctx.addInitScript(STEALTH_SCRIPT)
                    ctx
                }
            logger.info { "Camoufox strategy initialized" }
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

    private fun randomDelay() {
        Thread.sleep(Random.nextLong(1000L, 3000L))
    }

    override fun close() {
        logger.info { "Closing Camoufox strategy..." }
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
