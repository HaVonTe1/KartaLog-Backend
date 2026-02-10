package io.github.havonte1.tcgwatcher.backend

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tests to validate product uniqueness, updates, and associations across searches.
 */
@SpringBootTest
@Testcontainers
class SearchResultProductBehaviorTest {

    @TestConfiguration
    class ScraperTestConfig {
        private val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        private val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        @Bean
        @Primary
        fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
            var callCount = 0
            override fun search(searchString: String): List<Product> {
                callCount++
                class TestFetcher : CardMarketWebFetcherPort {
                    override fun fetch(searchString: String): String {
                        // First two calls return pikachu30. Subsequent calls return pikachu40
                        // to simulate updated prices.
                        return when (callCount) {
                            1, 2 -> Files.readString(Paths.get(testFilePikachu30))
                            else -> Files.readString(Paths.get(testFilePikachu40))
                        }
                    }
                }
                val adapter = CardMarketScraperAdapter(TestFetcher())
                return adapter.search(searchString)
            }
        }
    }

    @Autowired
    lateinit var service: SearchUseCase

    @Autowired
    lateinit var searchRepo: SearchResultRepository

    @Autowired
    lateinit var productRepo: ProductRepository

    @Autowired
    lateinit var scraperPort: CardMarketScraperPort

    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
    }

    @Test
    fun `products are unique and updated when changed`() {
        val f30 = "src/test/resources/pikachu_gallery_30.html"
        val f40 = "src/test/resources/pikachu_gallery_40.html"
        Assumptions.assumeTrue(Files.exists(Paths.get(f30)))
        Assumptions.assumeTrue(Files.exists(Paths.get(f40)))

        // 1) Perform first search -> should persist 30 products
        val resA = service.search("Pikachu30")
        assertEquals(30, resA.size)
        val allProductsAfterA = productRepo.findAll()
        assertEquals(30, allProductsAfterA.size, "All unique products should be persisted once")

        // 2) Different search with overlapping results should not create duplicates
        val resB = service.search("Pikachu30-other")
        assertEquals(30, resB.size)
        val allProductsAfterB = productRepo.findAll()
        assertEquals(30, allProductsAfterB.size)

        // 3) Now simulate a scraper run that returns the same products but with changed prices.
        val resC = service.search("Pikachu-Updated")
        assertEquals(30, resC.size)

        // Verify at least one product had its price updated in the DB.
        val exampleExternalId = 576753L
        val productBefore = allProductsAfterA.find { it.externalId == exampleExternalId }
        val productAfter = productRepo.findByExternalId(exampleExternalId)
        assertNotNull(productBefore)
        assertNotNull(productAfter)
        assertTrue(
            productBefore!!.price != productAfter!!.price,
            "Product price should be updated in DB when changed by scraper"
        )

        // 4) Ensure every unique product persisted exactly once
        val groupedByExternal = productRepo.findAll().groupBy { it.externalId }
        assertTrue(groupedByExternal.values.all { it.size == 1 }, "Each externalId must map to exactly one DB product")
    }

    companion object {
        @org.testcontainers.junit.jupiter.Container
        @org.springframework.boot.testcontainers.service.connection.ServiceConnection
        @JvmStatic
        val postgres = org.testcontainers.postgresql.PostgreSQLContainer("postgres:15-alpine")
    }
}
