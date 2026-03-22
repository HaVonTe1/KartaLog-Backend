package io.github.havonte1.tcgwatcher.backend.domain.model

/**
 * Domain model representing a cached search result.
 * It contains the original query string and the list of products that were
 * returned by the scraper for that query.
 */
import java.time.Instant

data class SearchResult(
    val id: Long? = null,
    val query: String,
    val products: List<Product> = emptyList(),
    /** Timestamp when this result was cached */
    val cachedAt: Instant? = null,
)
