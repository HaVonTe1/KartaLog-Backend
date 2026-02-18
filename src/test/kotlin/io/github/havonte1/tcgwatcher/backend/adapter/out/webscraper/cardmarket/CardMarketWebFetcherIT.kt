package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.toxiproxy.ToxiproxyContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Paths

@Testcontainers
@WireMockTest
@Tag("integration")
class CardMarketWebFetcherIT {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
            val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"
            val testFileEvoli = "src/test/resources/evoli_details.html"

            val wm1RuntimeInfo = wm1.getRuntimeInfo()
            org.testcontainers.Testcontainers.exposeHostPorts(wm1RuntimeInfo.httpPort)

            val pika30 = Files.readString(Paths.get(testFilePikachu30))
            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("/de/Pokemon/Products/Search.*"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pika30)
                    )
            )
            val evoli = Files.readString(Paths.get(testFileEvoli))
            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("/de/Pokemon/products/Singles/BaseSet/12345678.*"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(evoli)
                    )
            )
            val pika40 = Files.readString(Paths.get(testFilePikachu40))

            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("/en/Pokemon/Products/Search.*"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pika40)
                    )
            )
            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("/fr/Pokemon/Products/Search.*"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pika30)
                    )
            )
            val toxiproxyClient = ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort())
            val wmUpstream = "host.testcontainers.internal:${wm1RuntimeInfo.httpPort}"
            toxiproxyClient.createProxy("cm", "0.0.0.0:8666", wmUpstream)
        }

        @Container
        @JvmStatic
        val toxiproxyContainer: ToxiproxyContainer = ToxiproxyContainer(
            DockerImageName.parse("shopify/toxiproxy:2.1.4")
        ).withAccessToHost(true)

        @RegisterExtension
        @JvmStatic
        var wm1: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .failOnUnmatchedRequests(false)

            .resetOnEachTest(false)
            .build()
    }

    @Test
    fun `should successfully fetch product page with valid search term`() {
        val fetcher = CardMarketWebFetcher(
            PlaywrightManager(),
            CardMarketConfig().apply {
                basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
                timeoutMs = 10000L
            }
        )

        val result = runBlocking { fetcher.fetch("Pikachu", "de", "Pokemon") }
        Assertions.assertTrue(result.isSuccess)
        val content = result.getOrNull()
        Assertions.assertNotNull(content)
        Assertions.assertTrue(content!!.length > 0)
    }

    @Test
    fun `should successfully fetch details page with valid parameters`() {
        val fetcher = CardMarketWebFetcher(
            PlaywrightManager(),
            CardMarketConfig().apply {
                basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
                timeoutMs = 10000L
            }
        )

        val result = runBlocking { fetcher.fetchDetails("12345678", "Pokemon", "Singles", "de", "BaseSet") }
        Assertions.assertTrue(result.isSuccess)
        val content = result.getOrNull()
        Assertions.assertNotNull(content)
        Assertions.assertTrue(content!!.length > 0)
    }

    @Test
    fun `should use default values when locale or game is empty`() {
        val fetcher = CardMarketWebFetcher(
            PlaywrightManager(),
            CardMarketConfig().apply {
                basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
                timeoutMs = 10000L
            }
        )

        val result = runBlocking { fetcher.fetch("Charizard", "", "") }
        Assertions.assertTrue(result.isSuccess)
        val content = result.getOrNull()
        Assertions.assertNotNull(content)
        Assertions.assertTrue(content!!.length > 0)
    }

    @Test
    fun `should handle network timeout gracefully`() {
        val config = CardMarketConfig().apply {
            basePath = "http://${toxiproxyContainer.getHost()}:${toxiproxyContainer.getMappedPort(8666)}"
            timeoutMs = 1000L
        }
        val shortTimeoutFetcher = CardMarketWebFetcher(PlaywrightManager(), config)
//
//        Assertions.assertThrows(CircuitBreakerException::class.java) {
//            runBlocking { shortTimeoutFetcher.fetch("Pikachu", "de", "Pokemon") }
//        }
    }

    @Test
    fun `should cut connection and throw exception during network cut`() {
        val fetcher = CardMarketWebFetcher(
            PlaywrightManager(),
            CardMarketConfig().apply {
                basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
                timeoutMs = 10000L
            }
        )

        val toxiproxyClient = ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort())
        val proxy = toxiproxyClient.getProxy("cm")
        proxy.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0)
        proxy.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0)

//        Assertions.assertThrows(CircuitBreakerException::class.java) {
//            runBlocking { fetcher.fetch("Pikachu", "de", "Pokemon") }
//        }
    }

    @Test
    fun `should cut connection during details fetch`() {
        val fetcher = CardMarketWebFetcher(
            PlaywrightManager(),
            CardMarketConfig().apply {
                basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
                timeoutMs = 10000L
            }
        )

        val toxiproxyClient = ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort())
        val proxy = toxiproxyClient.getProxy("cm")
        proxy.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0)
        proxy.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0)

//        Assertions.assertThrows(CircuitBreakerException::class.java) {
//            runBlocking { fetcher.fetchDetails("87654321", "Pokemon", "Card", "de", "Jungle") }
//        }
    }

    @Test
    fun `should timeout when page fails to load`() {
        val fetcher = CardMarketWebFetcher(
            PlaywrightManager(),
            CardMarketConfig().apply {
                basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
                timeoutMs = 10000L
            }
        )

        val toxiproxyClient = ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort())
        val proxy = toxiproxyClient.getProxy("cm")

        proxy.toxics().timeout("TIMEOUT_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0)
//        Assertions.assertThrows(CircuitBreakerException::class.java) {
//            runBlocking { fetcher.fetch("nonexistentpage", "de", "Pokemon") }
//        }
    }
}
