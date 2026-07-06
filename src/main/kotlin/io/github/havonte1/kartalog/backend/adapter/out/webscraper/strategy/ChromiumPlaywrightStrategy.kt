package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.BrowserContextPool
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket.CloudFlareException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatusCode
import kotlin.random.Random

class ChromiumPlaywrightStrategy(
    private val executablePath: String? = null,
    private val poolSize: Int = 3,
    private val maxConcurrent: Int = 3,
) : ScrapingStrategy {
    private val logger = KotlinLogging.logger {}

    override val id: String = "chromium"
    override val displayName: String = "Java Playwright - Chromium"
    override val isAvailable: Boolean = true

    private var playwrightRef: Playwright? = null
    private var browserRef: Browser? = null
    private var contextPoolRef: BrowserContextPool? = null

    private fun ensureInitialized() {
        if (playwrightRef == null) {
            playwrightRef = Playwright.create()
            val exePath = executablePath
            val options =
                BrowserType.LaunchOptions().apply {
                    setHeadless(true)
                    if (!exePath.isNullOrBlank()) {
                        setExecutablePath(java.nio.file.Paths.get(exePath))
                    }
                    setArgs(
                        listOf(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-gpu",
                            "--disable-dev-shm-usage",
                            "--disable-software-rasterizer",
                            "--no-first-run",
                            "--no-zygote",
                            "--disable-background-networking",
                            "--disable-default-apps",
                            "--disable-sync",
                            "--disable-translate",
                            "--metrics-recording-only",
                            "--mute-audio",
                            "--disable-blink-features=AutomationControlled",
                            "--headless=new",
                        )
                    )
                }
            browserRef = playwrightRef!!.chromium().launch(options)
            val ctxOptions =
                Browser.NewContextOptions()
                    .setGeolocation(52.5200, 13.4050)
                    .setPermissions(listOf("geolocation"))
                    .setUserAgent(CHROMIUM_USER_AGENT)
                    .setLocale("de-DE")
                    .setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT)
                    .setViewportSize(1280, 720)
            contextPoolRef =
                BrowserContextPool(browserRef!!, poolSize, maxConcurrent) { br ->
                    val ctx = br.newContext(ctxOptions)
                    ctx.addInitScript(SHADOW_ROOT_PATCH)
                    ctx.addInitScript(STEALTH_SCRIPT)
                    ctx
                }
            logger.info { "Chromium strategy initialized" }
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
                try {
                    page.navigate(url, com.microsoft.playwright.Page.NavigateOptions().setTimeout(15000.0))
                } catch (_: com.microsoft.playwright.TimeoutError) {
                    logger.info { "Navigation timed out (expected with Cloudflare), starting poll..." }
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
                            && !html.contains("Just a moment") && !html.contains("Attention Required")) {
                            detected = true
                            break
                        }
                    } catch (_: Exception) {
                        logger.info { "Page evaluate failed, retrying..." }
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
        logger.info { "Closing Chromium strategy..." }
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
        private const val CHROMIUM_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"

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
