package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketScraperAdapter
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketContentParser
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketProductMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

import org.springframework.http.HttpHeaders
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
class CollectablesAdapterIT {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var cacheManager: CacheManager

    @BeforeEach
    fun clearCache() {
        cacheManager.getCacheNames().forEach { cacheManager.getCache(it)?.clear() }
    }

    @Test
    fun `GET collectables returns empty list on successful request`() {

        val mvcResult = mockMvc.get("/collectables/") {
            param("query", "test")
            param("locale", "en")
            param("game", "Pokemon")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        assertEquals(200, dispatched.response.status)
    }

    @Test
    fun `GET collectables returns ETag and Cache-Control headers`() {
        val mvcResult = mockMvc.get("/collectables/") {
            param("query", "Pikachu")
            param("locale", "en")
            param("game", "Pokemon")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        assertEquals(200, dispatched.response.status)
        assertTrue(dispatched.response.getHeader(HttpHeaders.ETAG) != null, "ETag header should be present")
        assertTrue(dispatched.response.getHeader(HttpHeaders.CACHE_CONTROL)?.contains("max-age") == true, "Cache-Control header should contain max-age")
    }

    @Test
    fun `GET collectables returns 304 when ETag matches`() {
        val firstRequest = mockMvc.get("/collectables/") {
            param("query", "Pikachu")
            param("locale", "en")
            param("game", "Pokemon")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatchedFirst = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(firstRequest)
        ).andReturn()

        assertEquals(200, dispatchedFirst.response.status)
        val eTag = dispatchedFirst.response.getHeader(HttpHeaders.ETAG) ?: ""

        val secondRequest = mockMvc.get("/collectables/") {
            param("query", "Pikachu")
            param("locale", "en")
            param("game", "Pokemon")
            header(HttpHeaders.IF_NONE_MATCH, eTag)
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatchedSecond = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(secondRequest)
        ).andReturn()

        assertEquals(304, dispatchedSecond.response.status)
    }

    @Test
    fun `GET product details returns ETag and Cache-Control headers`() {
        val mvcResult = mockMvc.get("/collectables/Pikachu-MCD166") {
            param("setname", "McDonalds-Collection-2016")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        assertEquals(200, dispatched.response.status)
        assertTrue(dispatched.response.getHeader(HttpHeaders.ETAG) != null, "ETag header should be present")
        assertTrue(dispatched.response.getHeader(HttpHeaders.CACHE_CONTROL)?.contains("max-age") == true, "Cache-Control header should contain max-age")
    }

    @Test
    fun `GET product details returns 304 when ETag matches`() {
        val firstRequest = mockMvc.get("/collectables/Pikachu-MCD166") {
            param("setname", "McDonalds-Collection-2016")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatchedFirst = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(firstRequest)
        ).andReturn()

        assertEquals(200, dispatchedFirst.response.status)
        val eTag = dispatchedFirst.response.getHeader(HttpHeaders.ETAG) ?: ""

        val secondRequest = mockMvc.get("/collectables/Pikachu-MCD166") {
            param("setname", "McDonalds-Collection-2016")
            header(HttpHeaders.IF_NONE_MATCH, eTag)
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatchedSecond = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(secondRequest)
        ).andReturn()

        assertEquals(304, dispatchedSecond.response.status)
    }


    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @AutoConfigureCache
    class TestConfig {

        private val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
        private val testFilePikachuDetails = "src/test/resources/pikachu_mcd166_details_stripped.html"

        @Bean
        @Primary
        fun cardMarketScraperPort(): CardMarketScraperPort = object : CardMarketScraperPort {
            override suspend fun search(searchString: String, locale: String, game: String): List<Product> {
                val content = Files.readString(Paths.get(testFilePikachu30))
                val parser = CardMarketContentParser()
                val mapper = CardMarketProductMapper()
                val adapter = CardMarketScraperAdapter(object : CardMarketWebFetcherPort {
                    override fun fetch(searchString: String, locale: String, game: String): Result<String> {
                        return Result.success(content)
                    }

                    override fun fetchDetails(cmId: String, genre: String, type: String, lang: String, setname: String): Result<String> {
                        val detailsContent = Files.readString(Paths.get(testFilePikachuDetails))
                        return Result.success(detailsContent)
                    }
                })
                val result = parser.parseGalaryPage(content)
                return mapper.toProducts(result)
            }

            override suspend fun fetchProductDetails(cmId: String, genre: String, type: String, lang: String, setname: String): Product? {
                val content = Files.readString(Paths.get(testFilePikachuDetails))
                val parser = CardMarketContentParser()
                val mapper = CardMarketProductMapper()
                val detailsDto = parser.parseProductDetails(content, cmId, genre, type, lang, setname)
                return mapper.toProductDetails(detailsDto)
            }
        }
    }

}
