package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.havonte1.tcgwatcher.backend.config.CardMarketConfig
import io.github.havonte1.tcgwatcher.backend.domain.model.Genre
import io.github.havonte1.tcgwatcher.backend.domain.model.Locale
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductType
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Paths

@Suppress("LongMethod", "MaxLineLength")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@ActiveProfiles(value = ["test"])
class CardMarketScraperAdapterIT {
    @Autowired
    private lateinit var scraper: CardMarketScraperAdapter

    @Autowired
    private lateinit var wiremockServer: WireMockServer

    private val pokemonGenre: Genre = Genre.POKEMON

    @Test
    fun `search returns products with German locale`() {
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.GERMAN, pokemonGenre)
            }
        assertThat(result.products).isNotEmpty
        val first = result.products.first()
        assertThat(first.cmId).isNotNull
        assertThat(first.names).containsKey(Locale.GERMAN)
    }

    @Test
    fun `search returns products with English locale`() {
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.ENGLISH, pokemonGenre)
            }
        assertThat(result.products).isNotEmpty
    }

    @Test
    fun `search returns products with French locale`() {
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.FRENCH, pokemonGenre)
            }
        assertThat(result.products).isNotEmpty
    }

    @Test
    fun `search returns products for Pokemon genre`() {
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.GERMAN, pokemonGenre)
            }
        assertThat(result.products).isNotEmpty
        result.products.forEach { product ->
            assertThat(product.genre.pathParam).isEqualTo("Pokemon")
        }
    }

    @Test
    fun `search returns products with required fields`() {
        val result: SearchResult =
            runBlocking {
                scraper.search("Pikachu", Locale.GERMAN, pokemonGenre)
            }
        assertThat(result.products).isNotEmpty
        val first = result.products.first()
        assertThat(first.externalId).isNotNull
        assertThat(first.names).isNotEmpty
        assertThat(first.price).isNotNull
        assertThat(first.imgLink).isNotNull
    }

    @Test
    fun `fetchProductDetails returns product with German locale`() {
        val result: Product? =
            runBlocking {
                scraper.fetchProductDetails("12345678", pokemonGenre, ProductType.SINGLES, Locale.GERMAN, "BaseSet")
            }
        assertThat(result).isNotNull
        assertThat(result!!.names).containsKey(Locale.GERMAN)
    }

    @Test
    fun `fetchProductDetails returns product with English locale`() {
        val result: Product? =
            runBlocking {
                scraper.fetchProductDetails("12345678", pokemonGenre, ProductType.SINGLES, Locale.GERMAN, "BaseSet")
            }
        assertThat(result).isNotNull
    }

    @Test
    fun `fetchProductDetails returns product with French locale`() {
        val result: Product? =
            runBlocking {
                scraper.fetchProductDetails("12345678", pokemonGenre, ProductType.SINGLES, Locale.GERMAN, "BaseSet")
            }
        assertThat(result).isNotNull
    }

    @Test
    fun `fetchProductDetails returns product with sell offers`() {
        val result: Product? =
            runBlocking {
                scraper.fetchProductDetails("12345678", pokemonGenre, ProductType.SINGLES, Locale.GERMAN, "BaseSet")
            }
        assertThat(result).isNotNull
        assertThat(result!!.sellOffers).isNotNull
    }

    @Test
    fun `fetchProductDetails handles missing product details gracefully`() {
        val result: Product? =
            runBlocking {
                scraper.fetchProductDetails("not-found", pokemonGenre, ProductType.SINGLES, Locale.GERMAN, "BaseSet")
            }
        assertThat(result).isNull()
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun wiremockServer(): WireMockServer {
            val wireMockServer = WireMockServer(options().dynamicPort())
            wireMockServer.start()
            org.testcontainers.Testcontainers.exposeHostPorts(wireMockServer.port())
            setUpStubs(wireMockServer)
            return wireMockServer
        }

        @Bean
        @Primary
        fun cardMarketConfig(wireMockServer: WireMockServer): CardMarketConfig {
            val cfg = CardMarketConfig()
            cfg.basePath = "http://localhost:${wireMockServer.port()}"
            return cfg
        }

        @ServiceConnection
        @Bean
        fun postgres() = PostgreSQLContainer("postgres:18.1-alpine").withReuse(true)

        private fun setUpStubs(wm: WireMockServer) {
            val pikachuGallery = Files.readString(Paths.get("src/test/resources/pikachu_gallery_30.html"))
            val pikachuGalleryEn = Files.readString(Paths.get("src/test/resources/pikachu_gallery_40.html"))
            val evoliDetails = Files.readString(Paths.get("src/test/resources/evoli_details_stripped.html"))

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/de/Pokemon/Products/Search"))
                    .withQueryParam("searchString", WireMock.equalTo("Pikachu"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pikachuGallery),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/en/Pokemon/Products/Search"))
                    .withQueryParam("searchString", WireMock.equalTo("Pikachu"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pikachuGalleryEn),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/fr/Pokemon/Products/Search"))
                    .withQueryParam("searchString", WireMock.equalTo("Pikachu"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(pikachuGallery),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/de/Pokemon/Products/Search"))
                    .withQueryParam("searchString", WireMock.equalTo("no-pagination"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody("<html><body><div id=\"pagination\"><span>no pagination data here</span></div></body></html>"),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/de/Pokemon/Products/Search"))
                    .withQueryParam("searchString", WireMock.equalTo("bad-html"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody("<html><body><div>no product tiles here</div></body></html>"),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/de/Pokemon/Products/Singles/BaseSet/12345678"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(evoliDetails),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/en/Pokemon/Products/Singles/BaseSet/12345678"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(evoliDetails),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/fr/Pokemon/Products/Singles/BaseSet/12345678"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody(evoliDetails),
                    ),
            )

            wm.stubFor(
                WireMock
                    .get(WireMock.urlPathEqualTo("/de/Pokemon/Products/Singles/BaseSet/not-found"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=UTF-8")
                            .withBody("<html><body><h1>Not Found</h1></body></html>"),
                    ),
            )
        }
    }
}
