package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import com.microsoft.playwright.Page
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlin.random.Random

object CloudflareChallengeSolver {
    private val logger = KotlinLogging.logger {}

    private val DETECT_JS = """
    (() => {
        const d = document;
        if (d.querySelector('input[name="cf-turnstile-response"]')) return true;
        if (d.querySelector('script[src*="/cdn-cgi/challenge-platform/"]')) return true;
        if (d.querySelector('#challenge-running')) return true;
        if (d.querySelector('.cf-turnstile')) return true;
        function walk(root) {
            const nodes = root.querySelectorAll('*');
            for (let i = 0; i < nodes.length; i++) {
                const el = nodes[i];
                if (!el.shadowRoot) continue;
                const sr = el.shadowRoot;
                if (sr.querySelector('input[name="cf-turnstile-response"]')) return true;
                if (sr.querySelector('iframe[src*="challenges.cloudflare.com"]')) return true;
                if (sr.querySelector('input[type="checkbox"]')) return true;
                if (walk(sr)) return true;
            }
            return false;
        }
        return walk(document);
    })()
    """.trimIndent()

    suspend fun hasChallenge(page: Page): Boolean {
        return try {
            page.evaluate(DETECT_JS) as Boolean
        } catch (e: Exception) {
            logger.warn { "Failed to detect challenge: ${e.message}" }
            false
        }
    }

    suspend fun trySolve(page: Page, maxWaitSeconds: Int = 60): Boolean {
        val deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L
        logger.info { "Attempting Cloudflare challenge solve (${maxWaitSeconds}s timeout)..." }

        while (System.currentTimeMillis() < deadline) {
            val cfFrames = page.frames().filter {
                it.url().contains("challenges.cloudflare.com")
            }
            if (cfFrames.isEmpty()) {
                delay(1500)
                continue
            }

            val frame = cfFrames.first()
            if (frame.isDetached()) { delay(1500); continue }

            // Check if challenge is already resolved (inside the frame)
            try {
                val frameResolved = frame.evaluate("!!document.getElementById('success')") as Boolean
                if (frameResolved) {
                    logger.info { "Challenge resolved in frame" }
                    delay(2000)
                    return true
                }
            } catch (_: Exception) {}

            // Try clicking checkbox inside the frame
            try {
                val frameCheckbox = frame.locator("input[type='checkbox']").first()
                if (frameCheckbox.count() > 0 && frameCheckbox.isVisible()) {
                    frameCheckbox.click()
                    logger.info { "Clicked Turnstile checkbox" }
                    delay(3000)
                    continue
                }
            } catch (_: Exception) {}

            // Try JS inside frame: walk shadow DOM for checkbox
            try {
                val clicked = frame.evaluate("""
                (() => {
                    function walk(root) {
                        const nodes = root.querySelectorAll('*');
                        for (let i = 0; i < nodes.length; i++) {
                            const el = nodes[i];
                            if (!el.shadowRoot) continue;
                            const cb = el.shadowRoot.querySelector('input[type="checkbox"]');
                            if (cb && cb.offsetParent !== null) { cb.click(); return true; }
                            if (walk(el.shadowRoot)) return true;
                        }
                        return false;
                    }
                    return walk(document);
                })()
                """.trimIndent()) as Boolean
                if (clicked) {
                    logger.info { "Clicked checkbox via JS inside frame" }
                    delay(3000)
                    continue
                }
            } catch (_: Exception) {}

            // Check if challenge auto-resolved (top-level detection)
            try {
                val topResolved = page.evaluate("""
                (() => {
                    const d = document;
                    if (!d.querySelector('input[name="cf-turnstile-response"]') &&
                        !d.querySelector('script[src*="/cdn-cgi/challenge-platform/"]')) return true;
                    function walk(root) {
                        const nodes = root.querySelectorAll('*');
                        for (let i = 0; i < nodes.length; i++) {
                            const el = nodes[i];
                            if (el.shadowRoot && el.shadowRoot.querySelector('#success')) return true;
                            if (el.shadowRoot && walk(el.shadowRoot)) return true;
                        }
                        return false;
                    }
                    return walk(document);
                })()
                """.trimIndent()) as Boolean
                if (topResolved) {
                    logger.info { "Challenge auto-resolved" }
                    return true
                }
            } catch (_: Exception) {}

            delay(1500)
        }

        logger.warn { "Failed to solve challenge within ${maxWaitSeconds}s" }
        return page.frames().none { it.url().contains("challenges.cloudflare.com") }
    }
}
