package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import eu.rekawek.toxiproxy.Proxy
import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.toxiproxy.ToxiproxyContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("e2e")
class CardMarketWebFetcherIT {

    @Autowired
    private lateinit var fetcher: CardMarketWebFetcherPort

    @Autowired
    lateinit var proxyForWiremockFetcher: Proxy

    @Autowired
    lateinit var wiremockServer: WireMockServer


    companion object {
        @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
        private val mainThreadSurrogate = newSingleThreadContext("UI thread")

        @OptIn(ExperimentalCoroutinesApi::class)
        @BeforeAll
        @JvmStatic
        fun setUp() {
            Dispatchers.setMain(mainThreadSurrogate)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @AfterAll
        @JvmStatic
        fun tearDown() {
            Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
            mainThreadSurrogate.close()
        }
    }

    @Test
    fun `check basic fetch functionality`() {
        val result1 = runBlocking { fetcher.fetch("Pikachu", "de", "Pokemon") }
        Assertions.assertTrue(result1.isSuccess)
        val content = result1.getOrNull()
        Assertions.assertNotNull(content)
        Assertions.assertTrue(content!!.isNotEmpty())

        wiremockServer.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/de/Pokemon/Products/Search?searchString=Pikachu"))
        )
    }

    private fun logThrowable(label: String, throwable: Throwable) {
        println("$label exception=${throwable::class.java.name} message=${throwable.message}")
        var current = throwable.cause
        while (current != null) {
            println("$label cause=${current::class.java.name} message=${current.message}")
            current = current.cause
        }
    }

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @AutoConfigureCache
    class TestConfig {

        fun setUp(wm1: WireMockServer) {
            val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
            val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"
            val testFileEvoli = "src/test/resources/evoli_details_stripped.html"

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
                WireMock.get(WireMock.urlPathMatching("/de/Pokemon/Products/Singles/BaseSet/12345678.*"))
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
            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("/de/Pokemon/Products/Singles/BaseSet/cloudflare"))
                    .willReturn(
                        aResponse()
                            .withStatus(403)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody("you want api key? too bad - here have some cloudflare")
                    )
            )
            wm1.stubFor(
                WireMock.get(WireMock.urlPathMatching("/de/Pokemon/Products/Singles/BaseSet/unknown"))
                    .willReturn(
                        aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody("bro...")
                    )
            )
        }

        @Bean
        fun wiremockServer(): WireMockServer {
            val wireMockServer = WireMockServer(options().dynamicPort())
            wireMockServer.start()
            org.testcontainers.Testcontainers.exposeHostPorts(wireMockServer.port())
            setUp(wireMockServer)
            return wireMockServer
        }

        @Bean
        fun proxyForWiremockFetcher(toxiproxyContainer: ToxiproxyContainer, wireMockServer: WireMockServer): Proxy {
            val toxiproxyClient = ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort())
            val wmUpstream = "host.testcontainers.internal:${wireMockServer.port()}"
            return toxiproxyClient.createProxy("cm", "0.0.0.0:8666", wmUpstream)
        }

        @Bean
        fun toxiproxyContainer(): ToxiproxyContainer {
            val toxiproxyContainer = ToxiproxyContainer(
                DockerImageName.parse("shopify/toxiproxy:2.1.4")
            ).withAccessToHost(true)
            toxiproxyContainer.start()
            return toxiproxyContainer
        }

        @ServiceConnection
        @Bean
        fun postgres() = PostgreSQLContainer("postgres:18.1-alpine")

        @Bean
        @Primary
        fun cardMarketConfig(toxiproxyContainer: ToxiproxyContainer): CardMarketConfig {
            val cfg = CardMarketConfig()
            cfg.basePath = "http://localhost:${toxiproxyContainer.getMappedPort(8666)}"
            return cfg
        }
    }
}
