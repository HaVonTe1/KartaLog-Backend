package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSeries
import io.github.havonte1.tcgwatcher.backend.domain.model.ProductSet
import io.github.havonte1.tcgwatcher.backend.domain.model.SellOffer
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity

class CardMarketProductMapper {
    fun toProducts(result: SearchResultsPageDto<CardmarketProductGallaryItemDto>): List<Product> =
        result.results.map { item ->

            val parsedLink = parseLink(item.cmId)

            // externalId should be the filename (without extension) of the imgLink, e.g.
            // https://.../574073.jpg -> 574073
            val externalIdFromImg =
                run {
                    val lastSegment = item.imgLink.substringAfterLast('/')
                    lastSegment.substringBeforeLast('.', lastSegment).toLongOrNull() ?: 0L
                }

            Product(
                externalId = externalIdFromImg,
                set = ProductSet(setId = 0, cmCode = parsedLink.setCode ?: "", names = mapOf()),
                series =
                    item.series?.let {
                        ProductSeries(
                            seriesId = it.seriesId,
                            names = mapOf(it.languageCode to it.name),
                        )
                    },
                rarity = null,
                names = mapOf(item.name.languageCode to item.name.value),
                codeInfo = StringWithValidity(item.code.value, item.code.valid),
                genre = item.genre,
                type = item.type,
                cmId = parsedLink.id,
                imgLink = item.imgLink,
                price = item.price,
                priceTrendInfo = StringWithValidity(item.priceTrend.value, item.priceTrend.valid),
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
            set = ProductSet(setId = 0, cmCode = detailsDto.set.code, names = mapOf(detailsDto.name.languageCode to detailsDto.set.name)),
            series =
                detailsDto.series?.let {
                    ProductSeries(
                        seriesId = it.seriesId,
                        names = mapOf(it.languageCode to it.name),
                    )
                },
            rarity = detailsDto.rarity,
            names = mapOf(detailsDto.name.languageCode to detailsDto.name.value),
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
                        sellerName = sellOffer.sellerName,
                        sellerLocation = sellOffer.sellerLocation,
                        productLanguage = sellOffer.productLanguage,
                        special = sellOffer.special,
                        condition = sellOffer.condition,
                        amount = sellOffer.amount,
                        price = sellOffer.price,
                    )
                },
        )
    }

    private data class ParsedLink(
        val genre: String?,
        val type: String?,
        val setCode: String?,
        val id: String?,
    )

    private fun parseLink(cmId: String?): ParsedLink {
        // cmId in DTO may be a URI like "/Pokemon/Products/Singles/Evolving-Skies/Pikachu-V1-EVS049";
        // we  want the genre (Pokemon), the type (Singles) , the set (Evolving-Skies) and the  last segment (e.g. "Pikachu-V1-EVS049").
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
