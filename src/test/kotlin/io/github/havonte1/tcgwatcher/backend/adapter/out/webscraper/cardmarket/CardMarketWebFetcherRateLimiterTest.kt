package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CardMarketWebFetcherRateLimiterTest {

    private lateinit var fetcher: CardMarketWebFetcher

    @BeforeEach
    fun setup() {
        fetcher = CardMarketWebFetcher()
    }

    @Test
    fun `rateLimiterFallback returns failure when rate limit is triggered for fetch`() {
        val searchString = "Pikachu"
        val locale = "de"
        val game = "Pokemon"

        val result = fetcher.rateLimiterFallback(searchString, locale, game, Exception("Rate limit exceeded"))

        assertEquals(true, result.isFailure)
        assertEquals("Rate limit exceeded for fetch", result.exceptionOrNull()?.message)
    }

    @Test
    fun `rateLimiterFallback returns failure when rate limit is triggered for fetchDetails`() {
        val cmId = "123456"
        val genre = "Pokemon"
        val type = "Singles"
        val lang = "de"
        val setname = "CEL"

        val result = fetcher.rateLimiterFallback(cmId, genre, type, lang, setname, Exception("Rate limit exceeded"))

        assertEquals(true, result.isFailure)
        assertEquals("Rate limit exceeded for fetchDetails", result.exceptionOrNull()?.message)
    }
}
