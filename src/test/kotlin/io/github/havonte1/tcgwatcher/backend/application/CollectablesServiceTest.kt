package io.github.havonte1.tcgwatcher.backend.application

import io.github.havonte1.tcgwatcher.backend.config.CacheConfig
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

class CollectablesServiceTest {

    private val mockScraperPort: CardMarketScraperPort = mockk()
    private val mockSearchResultRepository: SearchResultRepository = mockk()

    private val cacheConfig: CacheConfig = CacheConfig().apply { ttlHours = 1 }

    private lateinit var service: CollectablesService

    @BeforeEach
    fun setUp() {
        service = CollectablesService(mockScraperPort, mockSearchResultRepository, cacheConfig)
    }

    @Test
    fun `search returns products when cache miss`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        val products = listOf(
            Product(externalId = 1L, cmId = "CM001", names = mapOf("de" to "Pikachu"), genre = "Pokemon", type = "Singles", price = "1,50 €")
        )

        every { mockSearchResultRepository.findByQuery(query) } returns null
        coEvery { mockScraperPort.search(query, locale, game) } returns products
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(1, result.size)
        assertEquals("CM001", result[0].cmId)
    }

    @Test
    fun `search returns cached products when cache hit`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        
        // Create a very recent cached result (just now)
        val cachedResult = SearchResult(
            id = 1L,
            query = query,
            products = listOf(Product(externalId = 1L, cmId = "CM001", names = mapOf("de" to "Pikachu"), genre = "Pokemon", type = "Singles", price = "1,50 €")),
            cachedAt = Instant.now()
        )

        every { mockSearchResultRepository.findByQuery(query) } returns cachedResult
        coEvery { mockScraperPort.search(query, locale, game) } throws AssertionError("Should not be called on cache hit")

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(1, result.size)
        assertEquals("CM001", result[0].cmId)
    }

    @Test
    fun `search refreshes cache when ttl expired`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        val oldProducts = listOf(Product(externalId = 1L, cmId = "CM001", names = mapOf("de" to "Pikachu"), genre = "Pokemon", type = "Singles", price = "1,50 €"))
        val newProducts = listOf(Product(externalId = 2L, cmId = "CM002", names = mapOf("de" to "Pikachu"), genre = "Pokemon", type = "Singles", price = "2,00 €"))

        // Return expired cache (1 hour and 1 second ago - definitely past TTL)
        val expiredCache = SearchResult(
            id = 1L,
            query = query,
            products = oldProducts,
            cachedAt = Instant.now().minusSeconds(3601)
        )

        every { mockSearchResultRepository.findByQuery(query) } returns expiredCache
        coEvery { mockScraperPort.search(query, locale, game) } returns newProducts
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(1, result.size)
        assertEquals("CM002", result[0].cmId)
    }

    @Test
    fun `search uses fresh cache when ttl not expired`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        
        // Create a cached result that's only 30 minutes old (within 1 hour TTL)
        val cachedAt = Instant.now().minusSeconds(1800)
        val products = listOf(Product(externalId = 1L, cmId = "CM001", names = mapOf("de" to "Pikachu"), genre = "Pokemon", type = "Singles", price = "1,50 €"))
        
        val freshCache = SearchResult(
            id = 1L,
            query = query,
            products = products,
            cachedAt = cachedAt
        )

        every { mockSearchResultRepository.findByQuery(query) } returns freshCache
        coEvery { mockScraperPort.search(query, locale, game) } throws AssertionError("Should not be called on cache hit")

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(1, result.size)
        assertEquals("CM001", result[0].cmId)
    }

    @Test
    fun `search handles empty results from scraper`() {
        val query = "Pikachu"
        val locale = "de"
        val game = "Pokemon"
        val emptyProducts: List<Product> = listOf()

        every { mockSearchResultRepository.findByQuery(query) } returns null
        coEvery { mockScraperPort.search(query, locale, game) } returns emptyProducts
        every { mockSearchResultRepository.save(any()) } answers { firstArg<SearchResult>() }

        val result = runBlocking { service.search(query, locale, game) }

        assertEquals(0, result.size)
    }
}
