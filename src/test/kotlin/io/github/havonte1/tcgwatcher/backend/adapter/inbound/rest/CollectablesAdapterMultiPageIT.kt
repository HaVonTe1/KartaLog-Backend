package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model.ProductDTO
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.ProductJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository.SearchResultJpaRepository
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket.CardMarketWebFetcherPort
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
@ActiveProfiles(value = ["test"])
class CollectablesAdapterMultiPageIT {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var productRepo: ProductJpaRepository

    @Autowired
    lateinit var searchRepo: SearchResultJpaRepository

    @BeforeEach
    fun clearCache() {
        productRepo.deleteAll()
        searchRepo.deleteAll()
        cacheManager.getCacheNames().forEach { cacheManager.getCache(it)?.clear() }
    }


    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @AutoConfigureCache
    class TestConfig {
        private val testFilePikachuDetails = "src/test/resources/pikachu_mcd166_details_minimal.html"

        private val testFileGlurakPage1 = "src/test/resources/glurak_de_100_page1_minimal.html"
        private val testFileGlurakPage2 = "src/test/resources/glurak_de_100_page2_minimal.html"
        private val testFileGlurakPage3 = "src/test/resources/glurak_de_100_page3_minimal.html"
        private val testFileGlurakPage4 = "src/test/resources/glurak_de_100_page4_minimal.html"

        private val testFileGlurakXPage1 = "src/test/resources/glurak_x_de_100_page1_minimal.html"

        @Bean
        @Primary
        fun cardMarketWebFetcherPort(): CardMarketWebFetcherPort =
            object : CardMarketWebFetcherPort {

                override suspend fun fetch(
                    searchString: String,
                    locale: Locale,
                    genre: Genre,
                    page: Int,
                ): Result<String> {
                    return when {
                        searchString == "glurak" && genre == Genre.POKEMON && page == 1 -> {
                            Result.success(Files.readString(Paths.get(testFileGlurakPage1)))
                        }

                        searchString == "glurak" && genre == Genre.POKEMON && page == 2 -> {
                            Result.success(Files.readString(Paths.get(testFileGlurakPage2)))
                        }

                        searchString == "glurak" && genre == Genre.POKEMON && page == 3 -> {
                            Result.success(Files.readString(Paths.get(testFileGlurakPage3)))
                        }

                        searchString == "glurak" && genre == Genre.POKEMON && page == 4 -> {
                            Result.success(Files.readString(Paths.get(testFileGlurakPage4)))
                        }

                        searchString == "glurak x" && genre == Genre.POKEMON && page == 1 -> {
                            Result.success(Files.readString(Paths.get(testFileGlurakXPage1)))
                        }

                        else -> fail { "Unexpected fetch call: searchString='$searchString', page=$page" }
                    }
                }

                override suspend fun fetchDetails(
                    cmId: String,
                    genre: Genre,
                    type: ProductType,
                    locale: Locale,
                    setname: String,
                ): Result<String> {
                    val content = Files.readString(Paths.get(testFilePikachuDetails))
                    return Result.success(content)
                }
            }
    }
}
