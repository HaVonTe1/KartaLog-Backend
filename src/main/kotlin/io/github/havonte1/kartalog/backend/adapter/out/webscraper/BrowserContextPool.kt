package io.github.havonte1.kartalog.backend.adapter.out.webscraper

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentLinkedQueue

class BrowserContextPool(
    private val browser: Browser,
    private val poolSize: Int,
    private val maxConcurrent: Int,
    private val contextOptions: (Browser) -> BrowserContext,
) {
    private val logger = KotlinLogging.logger {}
    private val pool = ConcurrentLinkedQueue<BrowserContext>()
    private val semaphore = Semaphore(maxConcurrent)
    private val createdContexts = mutableListOf<BrowserContext>()
    private var closed = false

    init {
        repeat(poolSize) {
            val context = contextOptions(browser)
            pool.offer(context)
            createdContexts.add(context)
        }
        logger.info { "BrowserContextPool initialized with $poolSize contexts, max $maxConcurrent concurrent" }
    }

    suspend fun <T> use(block: (BrowserContext) -> T): T = semaphore.withPermit {
        val context = pool.poll() ?: run {
            logger.warn { "Pool exhausted, creating temporary context" }
            contextOptions(browser)
        }
        try {
            block(context)
        } finally {
            if (!closed) {
                pool.offer(context)
            } else {
                context.close()
            }
        }
    }

    fun close() {
        closed = true
        createdContexts.forEach { ctx ->
            try {
                ctx.close()
            } catch (e: Exception) {
                logger.warn { "Error closing context: ${e.message}" }
            }
        }
        logger.info { "BrowserContextPool closed" }
    }
}
