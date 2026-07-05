package io.github.havonte1.kartalog.backend.domain.port.out
import io.github.havonte1.kartalog.backend.domain.model.SearchResult
import java.time.Instant

interface SearchResultRepository {

    fun deleteAll()

    fun getCachedAtByQueryLocaleAndGenre(query: String, language: String, genre: String): Instant?

    fun findByQueryLocaleAndGenre(query: String, language: String, genre: String): SearchResult?

    fun save(searchResult: SearchResult): SearchResult

    fun countByQuery(query: String): Int
}
