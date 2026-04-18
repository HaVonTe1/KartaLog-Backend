package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SearchResultEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SearchResultJpaRepository : JpaRepository<SearchResultEntity, Long> {

    fun findByQueryAndLanguageAndGenre(
        query: String,
        language: String,
        genre: String
    ): SearchResultEntity?

    @Query(
        "SELECT s.cachedAt FROM SearchResultEntity s " +
            "WHERE s.query = :query " +
            "AND s.language = :language " +
            "AND s.genre = :genre")
    fun findCachedAtByQueryAndLanguageAndGenre(
        query: String,
        language: String,
        genre: String
    ): Instant?

    fun countByQuery(query: String): Int
}
