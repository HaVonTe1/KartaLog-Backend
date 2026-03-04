package io.github.havonte1.tcgwatcher.backend.domain.model

import java.time.Instant

data class Product(
    val id: Long? = null,
    /** Identifier from CardMarket (or other external source) */
    val externalId: Long,
    /** Set ID for relation to sets table */
    val setId: Long? = null,
    /** Optional set name (e.g., "Base Set", "Sword & Shield") */
    val setName: String? = null,
    /** Series ID for relation to series table */
    val seriesId: Long? = null,
    /** Rarity string as provided by the source (e.g., "Rare", "Ultra Rare") */
    val rarity: String? = null,
    /** Creation timestamp */
    val createdAt: Instant? = null,
    /** Last update timestamp */
    val updatedAt: Instant? = null,
    /** Map of locale code to translated product name */
    val names: Map<String, String> = emptyMap(),

    /** Code value with validity flag */
    val codeInfo: StringWithValidity? = null,
    /** Genre of the product */
    val genre: String = "",
    /** Type of the product */
    val type: String = "",
    /** CardMarket identifier as string */
    val cmId: String? = null,
    /** Image link from CardMarket */
    val imgLink: String? = null,
    /** Price string from CardMarket */
    val price: String? = null,
    /** Price trend with validity flag */
    val priceTrendInfo: StringWithValidity? = null,
    /** List of sell offers for this product */
    val sellOffers: List<SellOffer>? = null
)

data class StringWithValidity(
    val value: String? = null,
    val valid: Boolean? = null
)

data class SellOffer(
    val sellerName: String,
    val sellerLocation: String,
    val productLanguage: String,
    val special: String,
    val condition: String,
    val amount: String,
    val price: String
)
