package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import eu.rekawek.toxiproxy.Proxy
import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeAll
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
import org.springframework.util.Assert
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.toxiproxy.ToxiproxyContainer
import org.testcontainers.utility.DockerImageName
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
    fun `check state of circuit breaker, ratelimiter and retry when fetching stuff under various conditions`() {
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
        assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(100)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)

        //  first call : should success
        val result1 = runBlocking { fetcher.fetch("Pikachu", "de", "Pokemon") }
        Assertions.assertTrue(result1.isSuccess)
        val content = result1.getOrNull()
        Assertions.assertNotNull(content)
        Assertions.assertTrue(content!!.isNotEmpty())

        wiremockServer.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/de/Pokemon/Products/Search?searchString=Pikachu"))
        )


        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)

        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(0)

        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)

       // assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(99)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)

        // now we cut the internet connection on both ways
        proxyForWiremockFetcher.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0)
        proxyForWiremockFetcher.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0)

        //  second call : should fail
        val result2 = try {
            runBlocking { fetcher.fetch("Pikachu", "de", "Pokemon") }
        } catch (e: Exception) {
            // we catch the exception because the test would fail otherwise even if it was handled by the breaker and retyer like we hope for
            logThrowable("No Bandwidth", e)
            Result.failure(e)
        }
        Assertions.assertTrue(result2.isFailure)
        wiremockServer.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/de/Pokemon/Products/Search?searchString=Pikachu"))
        )
        // ---
        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)

        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isEqualTo(2)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(1)

        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(1)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(0)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)

//        assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(98)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)

        // restore the connection
        proxyForWiremockFetcher.toxics().get("CUT_CONNECTION_DOWNSTREAM").remove()
        proxyForWiremockFetcher.toxics().get("CUT_CONNECTION_UPSTREAM").remove()


        // make a call when cloudflare decides to be an a*+hole again
        val result3 = try {
            runBlocking { fetcher.fetchDetails("cloudflare", "Pokemon", "Singles", lang = "de", setname = "BaseSet") }
        } catch (e: Exception) {
            logThrowable("cloudflare", e)
            Result.failure(e)
        }
        Assertions.assertTrue(result3.isFailure)
        wiremockServer.verify(
            exactly(3), //1 call and 2 retries
            getRequestedFor(urlEqualTo("/de/Pokemon/Products/Singles/BaseSet/cloudflare"))
        )
        // ---
        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)

        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isEqualTo(5)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(4)

        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(1)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(1)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)

        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)

        // what if the client searches for unknown stuff
        val result4 = try {
            runBlocking { fetcher.fetchDetails("unknown", "Pokemon", "Singles", lang = "de", setname = "BaseSet") }
        } catch (e: Exception) {
            // the requst should fail and the exception be logged but i dont want the breaker to be open
            // neither the retrier should do stupid things
            logThrowable("unknown", e)
            Result.failure(e)
        }
        Assertions.assertTrue(result4.isFailure)
        wiremockServer.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/de/Pokemon/Products/Singles/BaseSet/unknown"))
        )

        (0 until 10).forEach { i ->
            var result: Result<String>
            try {
                result = runBlocking { fetcher.fetchDetails("unknown", "Pokemon", "Singles", lang = "de", setname = "BaseSet") }
            } catch (e: Exception) {
                logThrowable("unknown-loop", e)
                result = Result.failure(e)
            }
            Assertions.assertTrue(result.isFailure)
        }
        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)

        wiremockServer.verify(
            exactly(11),
            getRequestedFor(urlEqualTo("/de/Pokemon/Products/Singles/BaseSet/unknown"))
        )
        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)

        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isEqualTo(5)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(4)

        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isEqualTo(12)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isEqualTo(1)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)

        // now we want to test that the breaker opens when cloudflare decides to stay an a**hole
        // in parallel !!
        runBlocking {

            (0 until 10).forEach { i ->

                launch(Dispatchers.Default) {  // Use Default dispatcher for concurrency
                    var result: Result<String>
                    try {
                        result =
                            fetcher.fetchDetails("cloudflare", "Pokemon", "Singles", lang = "de", setname = "BaseSet")
                    } catch (e: Exception) {
                        logThrowable("unknown-loop", e)
                        result = Result.failure(e)
                    }
                    Assertions.assertTrue(result.isFailure)
                }
            }

        }

        assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)
        assertThat(circuitBreaker.metrics.numberOfBufferedCalls).isGreaterThanOrEqualTo(10)
        assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
        assertThat(circuitBreaker.metrics.numberOfFailedCalls).isGreaterThanOrEqualTo(9)

        assertThat(retry.metrics.numberOfFailedCallsWithoutRetryAttempt).isGreaterThanOrEqualTo(21)
        assertThat(retry.metrics.numberOfFailedCallsWithRetryAttempt).isGreaterThanOrEqualTo(1)
        assertThat(retry.metrics.numberOfSuccessfulCallsWithRetryAttempt).isEqualTo(0)
        assertThat(rateLimiter.metrics.numberOfWaitingThreads).isEqualTo(0)
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
