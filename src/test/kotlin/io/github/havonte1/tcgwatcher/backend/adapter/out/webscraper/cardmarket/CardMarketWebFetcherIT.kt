package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import eu.rekawek.toxiproxy.ToxiproxyClient
import io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.PlaywrightManager
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.CardMarketScraperPort
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.toxiproxy.ToxiproxyContainer
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths


@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class CardMarketWebFetcherIT {

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired
    private lateinit var retryRegistry: RetryRegistry

    @Autowired
    private lateinit var rateLimiterRegistry: RateLimiterRegistry

    @Autowired
    private lateinit var fetcher: CardMarketWebFetcherPort



    @Test
    fun `should successfully fetch product page with valid search term`() {

        val circuitBreaker: CircuitBreaker = circuitBreakerRegistry.circuitBreaker("cardMarketCircuitBreaker")
        assertThat(circuitBreaker).isNotNull()
        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)
        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isEqualTo(0)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(0)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(0)

        val retry: Retry = retryRegistry.retry("cardMarketRetry")
        assertThat(retry).isNotNull()
        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)

        val rateLimiter = rateLimiterRegistry.rateLimiter("cardMarketRateLimiter")
        assertThat(rateLimiter).isNotNull()
        assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(10)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)


        // -- first call : should success
        val result = runBlocking { fetcher.fetch("Pikachu", "de", "Pokemon") }
        Assertions.assertTrue(result.isSuccess)
        val content = result.getOrNull()
        Assertions.assertNotNull(content)
        Assertions.assertTrue(content!!.isNotEmpty())
        // ---

        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)

        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(0)


        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)


        assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(9)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)

    }


    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    class TestConfig {

        fun setUp(wm1: WireMockServer) {
            val testFilePikachu30 = "src/test/resources/pikachu_gallery_30.html"
            val testFilePikachu40 = "src/test/resources/pikachu_gallery_40.html"
            val testFileEvoli = "src/test/resources/evoli_details.html"



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

        }




        @Bean
        fun toxiproxyContainer(): ToxiproxyContainer {
            val toxiproxyContainer = ToxiproxyContainer(
                DockerImageName.parse("shopify/toxiproxy:2.1.4")
            ).withAccessToHost(true)
            toxiproxyContainer.start()

            val wireMockServer = WireMockServer(options().dynamicPort())
            wireMockServer.start()
            org.testcontainers.Testcontainers.exposeHostPorts(wireMockServer.port())

            val toxiproxyClient = ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort())
            val wmUpstream = "host.testcontainers.internal:${wireMockServer.port()}"
            toxiproxyClient.createProxy("cm", "0.0.0.0:8666", wmUpstream)

            setUp(wireMockServer)

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
