package io.github.havonte1.kartalog.backend.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Paths
import kotlin.random.Random

@Component
class PlaywrightManager(
    @Value("\${playwright.executable-path:}") private val executablePath: String?,
    @Value("\${playwright.context-pool-size:3}") private val poolSize: Int,
    @Value("\${playwright.max-concurrent-requests:3}") private val maxConcurrent: Int,
    @Value("\${playwright.browser:chromium}") private val browserType: String,
    @Value("\${playwright.user-data-dir:}") private val userDataDir: String?,
) {
    private val logger = KotlinLogging.logger {}

    private val isCamoufox get() = browserType.equals("camoufox", ignoreCase = true)

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
                browserInstance = if (isCamoufox) launchCamoufox() else launchChromium()
                initialized = true
            }
            return browserInstance!!
        }

    private fun launchChromium(): Browser {
        val exePath = executablePath
        val options =
            BrowserType.LaunchOptions().apply {
                setHeadless(true)
                if (!exePath.isNullOrBlank()) {
                    setExecutablePath(Paths.get(exePath))
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
                    )
                )
            }
        return playwright.chromium().launch(options)
    }

    private fun launchCamoufox(): Browser {
        val exePath = executablePath
        val options =
            BrowserType.LaunchOptions().apply {
                setHeadless(true)
                val path = exePath.takeIf { !it.isNullOrBlank() } ?: "/opt/camoufox/camoufox-bin"
                setExecutablePath(Paths.get(path))
            }
        return playwright.firefox().launch(options)
    }

    fun getContextPool(): BrowserContextPool {
        if (contextPool == null) {
            val stealthScript = STEALTH_SCRIPT
            val contextOptions = createContextOptions()
            contextPool =
                BrowserContextPool(browser, poolSize, maxConcurrent) { br ->
                    val ctx: BrowserContext = br.newContext(contextOptions)
                    ctx.addInitScript(stealthScript)
                    ctx
                }
        }
        return contextPool!!
    }

    private fun createContextOptions(): Browser.NewContextOptions {
        val dataDir = userDataDir
        val options =
            Browser
                .NewContextOptions()
                .setGeolocation(BERLIN_LAT, BERLIN_LONG)
                .setPermissions(listOf("geolocation"))
                .setUserAgent(if (isCamoufox) FIREFOX_USER_AGENT else CHROMIUM_USER_AGENT)
                .setLocale("de-DE")
                .setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT)
        if (isCamoufox) {
            options.setViewportSize(null as com.microsoft.playwright.options.ViewportSize?)
        } else {
            options.setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        }
        if (!dataDir.isNullOrBlank()) {
            val storagePath = Paths.get("$dataDir/storage-state.json")
            if (storagePath.toFile().exists()) {
                options.setStorageStatePath(storagePath)
            }
        }
        return options
    }

    fun randomDelay() {
        val ms = Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS)
        Thread.sleep(ms)
    }

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

    private companion object {
        private const val BERLIN_LAT = 52.5200
        private const val BERLIN_LONG = 13.4050
        private const val VIEWPORT_WIDTH = 1280
        private const val VIEWPORT_HEIGHT = 720
        private const val MIN_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 3000L
        private const val CHROMIUM_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
        private const val FIREFOX_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"

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
