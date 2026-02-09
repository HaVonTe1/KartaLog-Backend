package io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.cardmarket

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.StringWithValidity


class CardMarketProductMapper {
    fun toProducts(result: SearchResultsPageDto): List<Product> {
        return result.results.map { item ->
            Product(
                externalId = item.cmId.toLongOrNull() ?: 0L,
                setName = null,
                rarity = null,
                imageUrl = item.imgLink,
                createdAt = null,
                updatedAt = null,
                names = mapOf(item.name.languageCode to item.name.value),
                codeInfo = StringWithValidity(item.code.value, item.code.valid),
                genre = item.genre,
                type = item.type,
                cmId = item.cmId,
                cmLink = item.cmLink,
                imgLink = item.imgLink,
                price = item.price,
                priceTrendInfo = StringWithValidity(item.priceTrend.value, item.priceTrend.valid)
            )
        }
    }

}
