package com.github.havonte1.domain.model

import java.time.Instant


data class Product(
    val id: Long = 0,
    /** Identifier from CardMarket (or other external source) */
    val externalId: Long,
    /** Optional set name (e.g., "Base Set", "Sword & Shield") */
    val setName: String? = null,
    /** Rarity string as provided by the source (e.g., "Rare", "Ultra Rare") */
    val rarity: String? = null,
    /** URL to an image of the card */
    val imageUrl: String? = null,
    /** Creation timestamp */
    val createdAt: Instant? = null,
    /** Last update timestamp */
    val updatedAt: Instant? = null,
    /** Map of locale code to translated product name */
    val names: Map<String, String> = emptyMap(),

    /** Code value with validity flag */
    val codeInfo: StringWithValidity? = null,
    /** Genre of the product */
    val genre: String? = null,
    /** Type of the product */
    val type: String? = null,
    /** CardMarket identifier as string */
    val cmId: String? = null,
    /** Direct link to CardMarket product page */
    val cmLink: String? = null,
    /** Image link from CardMarket */
    val imgLink: String? = null,
    /** Price string from CardMarket */
    val price: String? = null,
    /** Price trend with validity flag */
    val priceTrendInfo: StringWithValidity? = null
) {

}

data class StringWithValidity(
    val value: String? = null,
    val valid: Boolean? = null
)
