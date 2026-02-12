package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity

class CardMarketProductMapper {
    fun toProducts(result: SearchResultsPageDto<CardmarketProductGallaryItemDto>): List<Product> {
        return result.results.map { item ->
            // cmId in DTO may be a URI like "/Pokemon/Products/Singles/Evolving-Skies/Pikachu-V1-EVS049";
            // we only want the last segment (e.g. "Pikachu-V1-EVS049").
            val parsedCmId = item.cmId.substringAfterLast('/').ifEmpty { item.cmId }

            // externalId should be the filename (without extension) of the imgLink, e.g.
            // https://.../574073.jpg -> 574073
            val externalIdFromImg = run {
                val lastSegment = item.imgLink.substringAfterLast('/')
                lastSegment.substringBeforeLast('.', lastSegment).toLongOrNull() ?: 0L
            }

            Product(
                externalId = externalIdFromImg,
                setName = null,
                rarity = null,
                createdAt = null,
                updatedAt = null,
                names = mapOf(item.name.languageCode to item.name.value),
                codeInfo = StringWithValidity(item.code.value, item.code.valid),
                genre = item.genre,
                type = item.type,
                cmId = parsedCmId,
                imgLink = item.imgLink,
                price = item.price,
                priceTrendInfo = StringWithValidity(item.priceTrend.value, item.priceTrend.valid)
            )
        }
    }
}
