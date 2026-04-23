package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.SearchResultMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import io.github.havonte1.tcgwatcher.backend.domain.port.out.SearchResultRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class SearchResultRepositoryAdapter(
    private val searchResultJpaRepository: SearchResultJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val searchResultMapper: SearchResultMapper,
    private val productRepositoryAdapter: ProductRepositoryAdapter,
) : SearchResultRepository {

    @Transactional(readOnly = true)
    override fun getCachedAtByQueryLocaleAndGenre(query: String, language: String, genre: String): Instant? {
        return searchResultJpaRepository.findCachedAtByQueryAndLanguageAndGenre(query, language, genre)
    }

    @Transactional(readOnly = true)
    override fun findByQueryLocaleAndGenre(
        query: String,
        language: String,
        genre: String
    ): SearchResult? {
        return searchResultJpaRepository
            .findByQueryAndLanguageAndGenre(query, language, genre)?.let { searchResultMapper.toDomain(it) }
    }

    @Transactional
    override fun save(searchResult: SearchResult): SearchResult {
        val searchResultEntity =
            searchResultJpaRepository
                .findByQueryAndLanguageAndGenre(searchResult.query, searchResult.language, searchResult.genre)
                ?: searchResultMapper.toEntityWithoutProducts(searchResult)

        val savedProducts = productRepositoryAdapter.saveAll(searchResult.products)

        val managedProducts =
            productJpaRepository.findAllByExternalIdIn(savedProducts.map { it.externalId }).toMutableSet()

        val toRemove = searchResultEntity.products - managedProducts
        val toAdd = managedProducts - searchResultEntity.products

        searchResultEntity.products.removeAll(toRemove)
        searchResultEntity.products.addAll(toAdd)

        val saved = searchResultJpaRepository.save(searchResultEntity)

        return searchResultMapper.toDomain(saved)
    }

    @Transactional
    override fun deleteAll() {
        searchResultJpaRepository.deleteAll()
    }

    override fun countByQuery(query: String): Int = searchResultJpaRepository.countByQuery(query)
}
