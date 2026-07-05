package io.github.havonte1.kartalog.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.kartalog.backend.domain.model.Genre
import io.github.havonte1.kartalog.backend.domain.model.Locale
import io.github.havonte1.kartalog.backend.domain.model.Product
import io.github.havonte1.kartalog.backend.domain.model.ProductType
import io.github.havonte1.kartalog.backend.domain.model.SearchResult
import io.github.havonte1.kartalog.backend.domain.port.out.CardMarketScraperPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant


@Component
class CardMarketScraperAdapter(
    private val webFetcher: CardMarketWebFetcherPort,
    private val galleryParser: CardMarketGalleryParser = CardMarketGalleryParser(),
    private val detailsParser: CardMarketDetailsParser = CardMarketDetailsParser(),
    private val mapper: CardMarketProductMapper = CardMarketProductMapper(),
) : CardMarketScraperPort {
    private val logger = KotlinLogging.logger {}

    override suspend fun search(
        searchString: String,
        locale: Locale,
        genre: Genre,
    ): SearchResult {
        logger.info { "Scraping CardMarket for $searchString" }

        var page = 1
        val maxPage = 100

        val result = fetchAndParse(searchString, locale, genre, page)
        logger.debug { "result: ${result.isSuccess} "}
        return result.fold(
            onSuccess = { resultsPageDto ->
                logger.debug { "page: ${resultsPageDto.page} of ${resultsPageDto.totalPages} - products=${resultsPageDto.results.size}" }
                val allProducts = arrayListOf<Product>()
                val products = mapper.toProducts(resultsPageDto)
                allProducts.addAll(products)
                while(page < resultsPageDto.totalPages && page < maxPage) {
                    page = page.inc()
                    fetchAndParse(searchString, locale, genre, page).fold(
                        onSuccess = { dto2 ->
                            allProducts.addAll(mapper.toProducts(dto2))
                        },
                        onFailure = {
                            logger.warn { "Failed to fetch page $page: ${it.message}" }
                        }
                    )
                }
                SearchResult(
                    query = searchString,
                    language = locale.code,
                    genre = genre.identifier,
                    products = allProducts,
                    cachedAt = Instant.now())
            },
            onFailure = { error ->
                logger.warn { "Failed to parse gallery page: ${error.message}" }
                throw error
            },
        )
    }


    private suspend fun fetchAndParse(
        searchString: String,
          locale: Locale,
          genre: Genre,
          page: Int
    )
    : Result<SearchResultsPageDto<CardmarketProductGallaryItemDto>> {
        logger.debug { "fetching and parsing query=$searchString locale=$locale genre=$genre page=$page" }
        val fetchResult = webFetcher.fetch(searchString, locale, genre, page)
        val content =
            fetchResult.getOrElse {
                logger.warn { "Failed to fetch CardMarket page: ${it.message}" }
                return Result.failure(it)
            }

        return galleryParser.parse(content, locale, page)
    }

    override suspend fun fetchProductDetails(
        cmId: String,
        genre: Genre,
        type: ProductType,
        locale: Locale,
        setname: String,
    ): Product? {
        logger.info { "Fetching product details for $cmId" }


        val fetchResult = webFetcher.fetchDetails(cmId, genre, type, locale, setname)
        val content =
            fetchResult.getOrElse {
                logger.warn { "Failed to fetch CardMarket detail page: ${it.message}" }
                return null
            }

        val result = detailsParser.parse(
            content,
            cmId,
            genre,
            type,
            locale,
        )
        return result.fold(
            onSuccess = { dto -> mapper.toProductDetails(dto) },
            onFailure = { error ->
                logger.warn { "Failed to parse product details: ${error.message}" }
                null
            },
        )
    }
}
