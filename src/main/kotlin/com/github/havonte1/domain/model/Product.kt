package com.github.havonte1.domain.model

import java.time.Instant

/**
 * Core domain aggregate representing a collectible card (e.g., a Pokémon card).
 *
 * * `externalId` – Identifier from the source provider (CardMarket).
 * * Multilingual textual values are stored in [LocalizedString] records.
 */
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
    /** Translatable strings linked to this product */
    val localizedStrings: MutableSet<LocalizedString> = mutableSetOf()
) {
    /** Helper to add a localized string. */
    fun addLocalizedString(ls: LocalizedString) {
        (localizedStrings as MutableSet).add(ls)
    }
}
