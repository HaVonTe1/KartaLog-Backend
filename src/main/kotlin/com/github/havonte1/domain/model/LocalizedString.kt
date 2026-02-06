package com.github.havonte1.domain.model

/**
 * Generic translatable text linked to a [Product] in the core domain.
 * The [type] field indicates the purpose of the text (e.g., "CARD_NAME").
 */
 data class LocalizedString(
    val id: Long = 0,
    val product: Product,
    val language: Language,
    val type: String,
    val value: String
)
