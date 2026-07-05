package io.github.havonte1.kartalog.backend.domain.model

import java.time.Instant

data class SearchResult(
    val id: Long? = null,
    val query: String,
    val language: String,
    val genre: String,
    val products: List<Product> = emptyList(),
    /** Timestamp when this result was cached */
    val cachedAt: Instant? = null,
)
