package io.github.havonte1.tcgwatcher.backend

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.fail

/**
 * Integration test for the cache‑aware CollectablesService.
 *
 * The web‑scraper port is replaced by a simple test implementation that reads a static HTML fixture.
 * The test verifies both a cache miss (scraper invoked) and a cache hit (scraper not invoked again).
 */
@SpringBootTest
@Testcontainers
class CollectablesServiceIntegrationTest {

    @TestConfiguration
    class ScraperTestConfig {

        private val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        private val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        @Bean
        @Primary // Override the real scraper bean with this test double
        fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
            var callCount = 0
            override suspend fun search(searchString: String): List<Product> {
                callCount++
                // Simple in‑memory fetcher that reads the fixture
                class TestFetcher : CardMarketWebFetcherPort {
                    override fun fetch(searchString: String): String {
                        if (callCount == 1) {
                            return Files.readString(Paths.get(testFilePikachu30))
                        }
                        return Files.readString(Paths.get(testFilePikachu40))
                    }
                }
                val adapter = CardMarketScraperAdapter(TestFetcher())
                return adapter.search(searchString)
            }
        }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
    }

    @Autowired
    lateinit var service: SearchUseCase

    @Autowired
    lateinit var searchRepo: SearchResultRepository

    @Autowired
    lateinit var productRepo: ProductRepository

    @Autowired
    lateinit var scraperPort: CardMarketScraperPort // will be the test double

    @BeforeEach
    fun cleanDb() {
        // Ensure a clean state before each test using repository deleteAll methods
        productRepo.deleteAll()
        searchRepo.deleteAll()
    }

    @Test
    fun `cache miss then hit`() {
        val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        // Ensure fixture exists – otherwise skip test
        Assumptions.assumeTrue(File(testFilePikachu30).exists())
        Assumptions.assumeTrue(File(testFilePikachu40).exists())

        // First call – cache miss, scraper should be invoked once
        val firstResult = service.search("Pikachu30")
        assertEquals(30, firstResult.size)
        // Cast scraper to access callCount
        val testScraper = scraperPort as? Any
        val callCountField = testScraper!!::class.java.getDeclaredField("callCount").apply { isAccessible = true }
        assertEquals(1, callCountField.getInt(testScraper))

        // Verify that a SearchResult has been persisted
        val cached = searchRepo.findByQuery("Pikachu30")
        assertEquals(30, cached?.products?.size)

        // Second call – cache hit, scraper must NOT be invoked again
        val secondResult = service.search("Pikachu30")
        assertEquals(30, secondResult.size)
        assertEquals(1, callCountField.getInt(testScraper), "Scraper should not be called on cache hit")

        secondResult.find { it.externalId == 576753L }?.let {
            assertEquals("Surfing-Pikachu-V", it.cmId)
            assertEquals("0,49 €", it.price)
        } ?: fail("No element with externalId=576753 found")
        // anothr query but the results are slightly different

        val thirdResult = service.search("Pikachu40")
        assertEquals(30, thirdResult.size)
        assertEquals(2, callCountField.getInt(testScraper), "Scraper should  be called on another query")

        thirdResult.find { it.externalId == 576753L }?.let {
            assertEquals("Surfing-Pikachu-V", it.cmId)
            assertEquals("2,34 €", it.price)
        } ?: fail("No element with externalId=576753 found")
    }
}
