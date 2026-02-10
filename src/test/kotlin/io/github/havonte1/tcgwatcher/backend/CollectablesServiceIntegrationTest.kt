package io.github.havonte1.tcgwatcher.backend

import com.microsoft.playwright.Playwright
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.ProductRepositoryAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.SearchResultRepositoryAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
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
import kotlin.use

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
        private val resourcePath = "src/test/resources/pikachu_gallery_50.html"

        @Bean
        @Primary // Override the real scraper bean with this test double
        fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
            var callCount = 0
            override fun search(searchString: String): List<Product> {
                callCount++
                // Simple in‑memory fetcher that reads the fixture
                class TestFetcher : CardMarketWebFetcherPort {
                    override fun fetch(searchString: String): String = Files.readString(Paths.get(resourcePath))
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
        val query = "Pikachu"
        // Ensure fixture exists – otherwise skip test
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_50.html").exists())

        // First call – cache miss, scraper should be invoked once
        val firstResult = service.search(query)
        assertEquals(30, firstResult.size)
        // Cast scraper to access callCount
        val testScraper = scraperPort as? Any
        val callCountField = testScraper!!::class.java.getDeclaredField("callCount").apply { isAccessible = true }
        assertEquals(1, callCountField.getInt(testScraper))

        // Verify that a SearchResult has been persisted
        val cached = searchRepo.findByQuery(query)
        assertEquals(30, cached?.products?.size)

        // Second call – cache hit, scraper must NOT be invoked again
        val secondResult = service.search(query)
        assertEquals(30, secondResult.size)
        assertEquals(1, callCountField.getInt(testScraper), "Scraper should not be called on cache hit")
    }
}
