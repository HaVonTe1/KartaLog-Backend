package io.github.havonte1.tcgwatcher.backend.domain.model

import java.time.Instant

data class Product(
    val id: Long? = null,
    /** Identifier from CardMarket (or other external source) */
    val externalId: Long,
    /** Source ID from SQLite quicksearch database */
    val sourceId: String? = null,
    val set: ProductSet? = null,
    val series: ProductSeries? = null,
    /** Rarity string as provided by the source (e.g., "Rare", "Ultra Rare") */
    val rarity: String? = null,
    /** Creation timestamp */
    val createdAt: Instant? = null,
    /** Last update timestamp */
    val updatedAt: Instant? = null,
    /** Map of locale code to translated product name */
    val names: Map<Locale, String> = emptyMap(),
    /** Code value with validity flag */
    val codeInfo: StringWithValidity? = null,
    /** Genre of the product */
    val genre: Genre,
    /** Type of the product */
    val type: ProductType,
    /** CardMarket identifier as string */
    val cmId: String? = null,
    /** Image link from CardMarket */
    val imgLink: String? = null,
    /** Price string from CardMarket */
    val price: String? = null,
    /** Price trend with validity flag */
    val priceTrendInfo: StringWithValidity? = null,
    /** List of sell offers for this product */
    val sellOffers: List<SellOffer>? = null,
    /** Language-specific pricing for all CardMarket languages */
    val languagePricing: List<LanguagePricing> = emptyList(),
    /** Additional product attributes from CardMarket */
    val productAttributes: List<ProductAttribute> = emptyList(),
    /** Release date from CardMarket */
    val releaseDate: String? = null,
    /** Card number in set from CardMarket */
    val cardNumber: String? = null,
)

data class StringWithValidity(
    val value: String? = null,
    val valid: Boolean? = null,
)

data class SellOffer(
    val sellerName: String,
    val sellerLocation: String,
    val productLanguage: String,
    val special: String,
    val condition: String,
    val amount: String,
    val price: String,
)

data class ProductSet(
    val setId: Long,
    val cmCode: String,
    val names: Map<Locale, String> = emptyMap(),
)

data class ProductSeries(
    val seriesId: Long,
    val names: Map<Locale, String> = emptyMap(),
)
