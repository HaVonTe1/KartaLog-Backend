/**
 * TCGWatcher API
 * Minimal OpenAPI contract for the TCGWatcher backend. This file will be expanded with the full use‑case APIs.
 *
 * The version of the OpenAPI document: 0.1.0
 */
package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid

/**
 * Data Transfer Object representing a product/collectable
 */
data class ProductDTO (

    @JsonProperty("id")
    @field:Valid
    val id: kotlin.Long? = null,

    @JsonProperty("externalId")
    @field:Valid
    val externalId: kotlin.Long? = null,

    @JsonProperty("setName")
    @field:Valid
    val setName: kotlin.String? = null,

    @JsonProperty("rarity")
    @field:Valid
    val rarity: kotlin.String? = null,

    @JsonProperty("imageUrl")
    @field:Valid
    val imageUrl: java.net.URI? = null,

    @JsonProperty("createdAt")
    @field:Valid
    val createdAt: java.time.OffsetDateTime? = null,

    @JsonProperty("updatedAt")
    @field:Valid
    val updatedAt: java.time.OffsetDateTime? = null
)
