package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.SearchResultMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * JPA implementation of the [SearchResultRepository] outbound port.
 */
@Component
class SearchResultRepositoryAdapter(
    private val jpaRepository: SearchResultJpaRepository,
    private val mapper: SearchResultMapper
) : SearchResultRepository {

    @Transactional(readOnly = true)
    override fun findByQuery(query: String): SearchResult? =
        jpaRepository.findByQuery(query)?.let { mapper.toDomain(it) }

    @Transactional
    override fun save(searchResult: SearchResult): SearchResult {
        val entity = mapper.toEntity(searchResult)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }
}
