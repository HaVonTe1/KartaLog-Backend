package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Paths

import org.springframework.http.HttpHeaders
import org.hamcrest.Matchers.containsString

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
class CollectablesAdapterIT {

    @Autowired
    lateinit var mockMvc: MockMvc


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

        fun setUp(wm1: WireMockServer) {
            val testFileEvoli = "src/test/resources/evoli_details_stripped.html"
            val testFilePikaChrome = "src/test/resources/pikachu_mcd166_details_stripped.html"

            val evoli = Files.readString(Paths.get(testFileEvoli))
            val pikaChrome = Files.readString(Paths.get(testFilePikaChrome))


            val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
            val pika30 = Files.readString(Paths.get(testFilePikachu30))

            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("(?i)/de/Pokemon/Products/Search.*"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pika30)
                    )
            )

            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("(?i)/en/Pokemon/Products/Search.*"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pika30)
                    )
            )

            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("(?i)/de/Pokemon/Products/Singles/McDonalds-Collection-2016/Pikachu-MCD166"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pikaChrome)
                    )
            )
            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("(?i)/de/Pokemon/Products/Singles/Pokemon-Jungle/Eevee"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(evoli)
                    )
            )
        }

        @Bean
        fun wiremockServer(): WireMockServer {
            val wireMockServer = WireMockServer(options().dynamicPort())
            wireMockServer.start()
            setUp(wireMockServer)
            return wireMockServer
        }


        @ServiceConnection
        @Bean
        fun postgres() = PostgreSQLContainer("postgres:18.1-alpine")

        @Bean
        @Primary
        fun cardMarketConfig(wireMockServer: WireMockServer): CardMarketConfig {
            val cfg = CardMarketConfig()
            cfg.basePath = "http://localhost:${wireMockServer.port()}"
            return cfg
        }
    }

}
