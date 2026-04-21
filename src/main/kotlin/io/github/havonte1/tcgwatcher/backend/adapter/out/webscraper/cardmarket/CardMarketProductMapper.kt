package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.LanguagePricing
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductAttribute
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductAttributeType
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSeries
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import io.github.havonte1.tcgwatcher.backend.domain.model.SellOffer
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.random.Random

class CardMarketProductMapper {
    val rng = Random(System.currentTimeMillis())
    val logger = KotlinLogging.logger {}
    fun toProducts(result: SearchResultsPageDto<CardmarketProductGallaryItemDto>): List<Product> =
        result.results.map { dto ->

            val parsedLink = parseLink(dto.cmId)

            val externalIdFromImg =
                run {
                    val lastSegment = dto.imgLink.substringAfterLast('/')
                    var lng = lastSegment.substringBeforeLast('.', lastSegment).toLongOrNull() ?: 0L

                    //if there is no image available this results to 0 which cannot be handled now
                    if(lng==0L) {
                        logger.warn { "no img link found for $dto - generating random externalid" }
                        lng = rng.nextLong()
                    }
                    lng
                }


            Product(
                externalId = externalIdFromImg,
                set = ProductSet(setId = 0, cmCode = parsedLink.setCode ?: "", names = mapOf()),
                rarity = null,
                names = mapOf(dto.name.locale to dto.name.value),
                codeInfo = StringWithValidity(
                    dto.code.value,
                    dto.code.valid
                ),
                genre = dto.genre,
                type = dto.type,
                cmId = parsedLink.id,
                imgLink = dto.imgLink,
                price = dto.price,
            )
        }

    fun toProductDetails(detailsDto: CardmarketProductDetailsDto): Product {
        val externalIdFromImg =
            run {
                val lastSegment = detailsDto.imageUrl.substringAfterLast('/')
                lastSegment.substringBeforeLast('.', lastSegment).toLongOrNull() ?: 0L
            }

        return Product(
            externalId = externalIdFromImg,
            set = ProductSet(
                setId = 0,
                cmCode = detailsDto.set.code,
                names = mapOf(detailsDto.name.locale to detailsDto.set.name)
            ),
            rarity = detailsDto.rarity,
            names = mapOf(detailsDto.name.locale to detailsDto.name.value),
            codeInfo = StringWithValidity(detailsDto.code.value, detailsDto.code.valid),
            genre = detailsDto.genre,
            type = detailsDto.type,
            cmId = detailsDto.cmId,
            imgLink = detailsDto.imageUrl,
            price = detailsDto.price,
            priceTrendInfo = StringWithValidity(detailsDto.priceTrend.value, detailsDto.priceTrend.valid),
            sellOffers =
                detailsDto.sellOffers.map { sellOffer ->
                    SellOffer(
                        sellOfferId = 0,
                        sellerName = sellOffer.sellerName,
                        sellerLocation = sellOffer.sellerLocation,
                        productLanguage = sellOffer.productLanguage,
                        special = sellOffer.special,
                        condition = sellOffer.condition,
                        amount = sellOffer.amount,
                        price = sellOffer.price,
                    )
                },
            languagePricing = detailsDto.languagePricing.map { lp ->
                LanguagePricing(
                    locale = lp.locale,
                    price = lp.price,
                    priceTrend = lp.priceTrend,
                    priceTrendValid = lp.priceTrendValid,
                )
            },
            productAttributes = detailsDto.productAttributes.map { pa ->
                ProductAttribute(
                    attributeName = pa.attributeName,
                    value = pa.value,
                    attributeType = ProductAttributeType.entries.find { it.name == pa.attributeType }
                        ?: ProductAttributeType.RARITY,
                )
            },
            releaseDate = detailsDto.releaseDate.ifEmpty { null },
            cardNumber = detailsDto.cardNumber.ifEmpty { null },
            series = if (detailsDto.seriesId != null && detailsDto.seriesName != null) {
                ProductSeries(
                    seriesId = detailsDto.seriesId,
                    names = mapOf(detailsDto.name.locale to detailsDto.seriesName)
                )
            } else null,
        )
    }

    private data class ParsedLink(
        val genre: String?,
        val type: String?,
        val setCode: String?,
        val id: String?,
    )

    private fun parseLink(cmId: String?): ParsedLink {
        if (cmId == null || cmId.trim().isEmpty()) {
            return ParsedLink(null, null, null, null)
        }

        val parts = cmId.split('/')

        return ParsedLink(
            parts.getOrNull(1),
            parts.getOrNull(3),
            parts.getOrNull(4),
            parts.getOrNull(5),
        )
    }
}
