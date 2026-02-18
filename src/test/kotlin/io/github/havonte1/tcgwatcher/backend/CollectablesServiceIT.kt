package io.github.havonte1.tcgwatcher.backend

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
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

@SpringBootTest
@Testcontainers
@Tag("integration")
class CollectablesServiceIT {

    @TestConfiguration
    class ScraperTestConfig {

        private val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        private val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        @Bean
        @Primary
        fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
            var callCount = 0
            override suspend fun search(searchString: String, locale: String, game: String): List<Product> {
                callCount++
                class TestFetcher : CardMarketWebFetcherPort {
                    override fun fetch(searchString: String, locale: String, game: String): Result<String> {
                        val content = if (callCount == 1) {
                            Files.readString(Paths.get(testFilePikachu30))
                        } else {
                            Files.readString(Paths.get(testFilePikachu40))
                        }
                        return Result.success(content)
                    }

                    override fun fetchDetails(
                        cmId: String,
                        genre: String,
                        type: String,
                        lang: String,
                        setname: String
                    ): Result<String> {
                        return Result.failure(UnsupportedOperationException("Not implemented"))
                    }

                }
                val adapter = CardMarketScraperAdapter(TestFetcher())
                return adapter.search(
                    searchString,
                    locale = locale,
                    game = game
                )
            }

            override suspend fun fetchProductDetails(cmId: String, genre: String, type: String, lang: String, setname: String): Product? {
                return null
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
    lateinit var scraperPort: CardMarketScraperPort

    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
    }

    @Test
    fun `cache miss then hit`() {
        val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        Assumptions.assumeTrue(File(testFilePikachu30).exists())
        Assumptions.assumeTrue(File(testFilePikachu40).exists())

        val firstResult = runBlocking { service.search("Pikachu30", "de", "Pokemon") }
        assertEquals(30, firstResult.size)
        val testScraper = scraperPort as? Any
        val callCountField = testScraper!!::class.java.getDeclaredField("callCount").apply { isAccessible = true }
        assertEquals(1, callCountField.getInt(testScraper))

        val cached = searchRepo.findByQuery("Pikachu30")
        assertEquals(30, cached?.products?.size)

        val secondResult = runBlocking { service.search("Pikachu30", "de", "Pokemon") }
        assertEquals(30, secondResult.size)
        assertEquals(1, callCountField.getInt(testScraper), "Scraper should not be called on cache hit")

        secondResult.find { it.externalId == 576753L }?.let {
            assertEquals("Pikachu-V4-CEL008", it.cmId)
            assertEquals("1,50 €", it.price)
        } ?: fail("No element with externalId=576753 found")

        val thirdResult = runBlocking {service.search("Pikachu40", "de", "Pokemon")}
        assertEquals(30, thirdResult.size)
        assertEquals(2, callCountField.getInt(testScraper), "Scraper should  be called on another query")

        thirdResult.find { it.externalId == 576753L }?.let {
            assertEquals("Pikachu-V4-CEL008", it.cmId)
            assertEquals("4,00 €", it.price)
        } ?: fail("No element with externalId=576753 found")
    }
}
