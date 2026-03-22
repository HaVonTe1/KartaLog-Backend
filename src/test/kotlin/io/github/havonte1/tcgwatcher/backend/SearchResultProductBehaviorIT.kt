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
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@Testcontainers
@Tag("integration")
@AutoConfigureCache
class SearchResultProductBehaviorIT {
    @TestConfiguration
    class ScraperTestConfig {
        private val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        private val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"

        @Bean
        @Primary
        fun cardMarketScraperPort(): CardMarketScraperPort =
            object : CardMarketScraperPort {
                var callCount = 0

                override suspend fun search(
                    searchString: String,
                    locale: String,
                    game: String,
                ): List<Product> {
                    callCount++

                    class TestFetcher : CardMarketWebFetcherPort {
                        override fun fetch(
                            searchString: String,
                            locale: String,
                            game: String,
                        ): Result<String> {
                            val content =
                                when (callCount) {
                                    1, 2 -> Files.readString(Paths.get(testFilePikachu30))
                                    else -> Files.readString(Paths.get(testFilePikachu40))
                                }
                            return Result.success(content)
                        }

                        override fun fetchDetails(
                            cmId: String,
                            genre: String,
                            type: String,
                            lang: String,
                            setname: String,
                        ): Result<String> = Result.failure(UnsupportedOperationException("Not implemented"))
                    }

                    val adapter = CardMarketScraperAdapter(TestFetcher())
                    return adapter.search(searchString, locale, game)
                }

                override suspend fun fetchProductDetails(
                    cmId: String,
                    genre: String,
                    type: String,
                    lang: String,
                    setname: String,
                ): Product? = null
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

        val firstResult = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(30, firstResult.size)
        assertEquals(30, productRepo.findAll().size)

        val secondResult = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(30, secondResult.size)
        assertEquals(1, searchRepo.countByQuery("Pikachu"))
    }

    @Test
    fun `products are updated when prices change`() {
        val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"
        Assumptions.assumeTrue(File(testFilePikachu40).exists())

        runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(30, productRepo.findAll().size)

        val secondResult = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(30, secondResult.size)
    }

    @Test
    fun `search results are cached correctly`() {
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_30.html").exists())

        runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(1, searchRepo.countByQuery("Pikachu"))

        val cachedResult = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        assertEquals(30, cachedResult.size)
        assertEquals(1, searchRepo.countByQuery("Pikachu"))
    }

    @Test
    fun `products maintain their relationships after caching`() {
        Assumptions.assumeTrue(File("src/test/resources/pikachu_gallery_30.html").exists())

        val firstSearch = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        val productId = firstSearch.first().externalId

        val cachedSearch = runBlocking { service.search("Pikachu", "de", "Pokemon") }
        val cachedProduct = cachedSearch.find { it.externalId == productId }

        assertNotNull(cachedProduct)
        assertEquals(firstSearch.first().cmId, cachedProduct?.cmId)
    }
}
