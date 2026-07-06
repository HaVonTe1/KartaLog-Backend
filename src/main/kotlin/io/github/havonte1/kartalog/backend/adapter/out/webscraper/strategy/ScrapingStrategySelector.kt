package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class ScrapingStrategySelector(
    private val registry: ScrapingStrategyRegistry,
    @Value("\${scraper.default-strategy:chromium}") defaultId: String,
) {
    private val logger = KotlinLogging.logger {}

    private val activeRef =
        AtomicReference(
            registry.get(defaultId)
                ?: error("Default strategy '$defaultId' not registered")
        )

    fun get(): ScrapingStrategy = activeRef.get()

    fun getActiveId(): String = activeRef.get().id

    fun getAll(): Collection<ScrapingStrategy> = registry.getAll()

    fun switch(id: String) {
        val next = registry.get(id) ?: throw IllegalArgumentException("Unknown strategy: $id")
        if (!next.isAvailable) {
            throw IllegalStateException("Strategy '$id' is not available")
        }
        val prev = activeRef.getAndSet(next)
        if (prev.id != next.id) {
            logger.info { "Switching scraping strategy: ${prev.id} -> ${next.id}" }
            prev.close()
        }
    }
}
