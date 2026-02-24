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
    fun `GET collectables with blank query returns server error`() {
        try {
            mockMvc.get("/collectables/") {
                param("query", "   ")
                param("locale", "en")
                param("game", "Pokemon")
            }
                .andExpect { request { asyncStarted() } }
        } catch (ex: Exception) {
            val message = ex.cause?.message ?: ex.message ?: ""
            assertTrue(
                message.contains("Query must not be blank"),
                "Expected exception message to mention blank query but was: $message"
            )
        }
    }


    @Test
    fun `GET collectables product details - successful request`() {
        val mvcResult = mockMvc.get("/collectables/Pikachu-MCD166") {
            param("setname", "McDonalds-Collection-2016")
        }
            .andExpect { request { asyncStarted() } }
            .andReturn()

        val dispatched = mockMvc.perform(
            MockMvcRequestBuilders.asyncDispatch(mvcResult)
        ).andReturn()

        assertEquals(200, dispatched.response.status)

        val responseBody = dispatched.response.contentAsString
        assertTrue(responseBody.contains("\"externalId\":295142"))
        assertTrue(responseBody.contains("\"setName\":\"McDonald's Collection 2016\""))
        assertTrue(responseBody.contains("\"rarity\":\"Promo\""))
        assertTrue(responseBody.contains("\"type\":\"Singles\""))
        assertTrue(responseBody.contains("\"genre\":\"pokemon\""))
        assertTrue(responseBody.contains("\"price\":\"1,40 €\""))
        assertTrue(responseBody.contains("\"priceTrend\":\"12,81 €\""))
        assertTrue(responseBody.contains("https://product-images.s3.cardmarket.com/51/MCD16/295142/295142.jpg"))
        assertTrue(responseBody.contains("\"sellOffers\":["))
    }


    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @AutoConfigureCache
    class TestConfig {

        fun setUp(wm1: WireMockServer) {
            val testFileEvoli = "src/test/resources/evoli_details.html"
            val testFilePikaChrome = "src/test/resources/pikachu_mcd166_details.html"

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
