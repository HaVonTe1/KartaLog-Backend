package io.github.havonte1.tcgwatcher.backend

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.application.SearchResponse
import io.github.havonte1.tcgwatcher.backend.application.SearchUseCase
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@Testcontainers
@Tag("integration")
@AutoConfigureCache
@ActiveProfiles(value = ["test"])
class SearchResultProductBehaviorIT {
    private val locale: Locale = Locale.GERMAN
    private val genre: Genre = Genre.POKEMON

    @TestConfiguration
    class ScraperTestConfig {
        private val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        private val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        @Bean
        @Primary
        fun cardMarketScraperPort(): CardMarketScraperPort {
            val testFilePikachu30Content = Files.readString(Paths.get(testFilePikachu30))
            val testFilePikachu40Content = Files.readString(Paths.get(testFilePikachu40))

            class TestCardMarketWebFetcher : CardMarketWebFetcherPort {
                override suspend fun fetch(
                    searchString: String,
                    locale: Locale,
                    genre: Genre,
                    page: Int,
                ): Result<String> {
                    val content =
                        if (searchString == "Pikachu") {
                            testFilePikachu30Content
                        } else {
                            testFilePikachu40Content
                        }
                    return Result.success(content)
                }

                override suspend fun fetchDetails(
                    cmId: String,
                    genre: Genre,
                    type: ProductType,
                    locale: Locale,
                    setname: String,
                ): Result<String> = Result.failure(UnsupportedOperationException("Not implemented"))
            }

            return CardMarketScraperAdapter(TestCardMarketWebFetcher())
        }

        @Bean
        @ServiceConnection
        fun postgres(): PostgreSQLContainer = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)
    }

    @Autowired
    lateinit var service: SearchUseCase

    @Autowired
    lateinit var searchRepo: SearchResultRepository

    @Autowired
    lateinit var productRepo: ProductRepository

    @Autowired
    lateinit var cacheManager: CacheManager

    @BeforeEach
    fun cleanDb() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
        cacheManager.getCacheNames().forEach { cacheManager.getCache(it)?.clear() }
    }

    @Test
    fun `products are unique by externalId`() {
        val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        Assumptions.assumeTrue(File(testFilePikachu30).exists())

        val firstResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, firstResult.products.size)
        assertEquals(30, productRepo.findAll().size)

        val secondResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, secondResult.products.size)
        assertEquals(1, searchRepo.countByQuery("Pikachu"))
    }

    @Test
    fun `products are updated when prices change`() {
        val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"
        Assumptions.assumeTrue(File(testFilePikachu40).exists())

        runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, productRepo.findAll().size)

        val secondResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, secondResult.products.size)
    }

    @Test
    fun `search results are cached correctly`() {
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_30.html").exists())

        runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(1, searchRepo.countByQuery("Pikachu"))

        val cachedResult: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        assertEquals(30, cachedResult.products.size)
        assertEquals(1, searchRepo.countByQuery("Pikachu"))
    }

    @Test
    fun `products maintain their relationships after caching`() {
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_30.html").exists())

        val firstSearch: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        val productId = firstSearch.products.first().externalId

        val cachedSearch: SearchResponse = runBlocking { service.search("Pikachu", locale, genre) }
        val cachedProduct = cachedSearch.products.find { it.externalId == productId }

        assertNotNull(cachedProduct)
        assertEquals(firstSearch.products.first().cmId, cachedProduct?.cmId)
    }
}
