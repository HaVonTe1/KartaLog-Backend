package io.github.havonte1.tcgwatcher.backend.domain.port.out

import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult

/**
 * Outbound port for persisting and retrieving cached search results.
 */
interface SearchResultRepository {
    /** Find a cached result by its query string. */
    fun findByQuery(query: String): SearchResult?

    /** Persist a [SearchResult] (insert or update) and return the managed instance. */
    fun save(searchResult: SearchResult): SearchResult
}
