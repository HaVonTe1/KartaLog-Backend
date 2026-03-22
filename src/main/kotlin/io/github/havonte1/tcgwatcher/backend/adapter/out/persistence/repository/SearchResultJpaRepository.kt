package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SearchResultEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data repository for [SearchResultEntity].
 */
@Repository
interface SearchResultJpaRepository : JpaRepository<SearchResultEntity, Long> {
    fun findByQuery(query: String): SearchResultEntity?

    fun countByQuery(query: String): Int
}
