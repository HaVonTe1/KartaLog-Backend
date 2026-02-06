package com.github.havonte1.adapter.out.webscraper

import com.github.havonte1.domain.model.Product

import com.github.havonte1.domain.model.StringWithValidity

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
