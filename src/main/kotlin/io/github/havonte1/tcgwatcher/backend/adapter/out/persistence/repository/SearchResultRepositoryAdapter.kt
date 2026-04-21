package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.SearchResultMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
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

    /**
     * Inserts new product entities or updates existing ones based on external ID.
     * Uses transaction isolation to prevent race conditions by ensuring all operations
     * happen within the same database transaction with consistent snapshot isolation.
     */
//    private fun upsertProducts(products: List<Product>): List<ProductEntity> {
//        productRepositoryAdapter.saveAll(products)
//
//        if (products.isEmpty()) return emptyList()
//
//        val externalIds = products.map { it.externalId }
//
//        // Query existing products in the same transaction to get a consistent view
//        val existingProducts =
//            productJpaRepository
//                .findAllByExternalIdIn(externalIds)
//                .associateBy { it.externalId }
//
//        val uniqueCmSetCodes = products.mapNotNull { it.set?.cmCode }.distinct()
//        val existingSets = productSetJpaRepository.findByCmProductCodeIn(uniqueCmSetCodes)
//        val existingSetMap = existingSets.associateBy { it.cmProductCode }
//
//        val newCmCodes = uniqueCmSetCodes.filter { it !in existingSetMap.keys }
//        val newSets =
//            newCmCodes.map { newCmSetCode ->
//                val product = products.first { it.set?.cmCode == newCmSetCode }
//                val productSetEntity = ProductSetEntity(cmProductCode = newCmSetCode)
//                product.set?.names?.forEach { (lang, name) ->
//                    val nameTranslationEntity =
//                        NameTranslationEntity(
//                            productSet = productSetEntity,
//                            languageCode = lang.code,
//                            name = name,
//                        )
//                    productSetEntity.nameTranslations.add(nameTranslationEntity)
//                }
//                productSetEntity
//            }
//
//        val savedNewSets =
//            if (newSets.isNotEmpty()) {
//                productSetJpaRepository.saveAll(newSets)
//            } else {
//                emptyList()
//            }
//
//        val allSetMap = (existingSets + savedNewSets).associateBy { it.cmProductCode }
//
//        val entitiesToPersist =
//            products.map { product ->
//                val existing = existingProducts[product.externalId]
//                if (existing != null) {
//                    // Update existing product with new data
//                    updateEntity(existing, product)
//                    existing
//                } else {
//                    val cmCode = product.set?.cmCode ?: "dummy"
//                    val productSetEntity =
//                        allSetMap[cmCode]
//                            ?: ProductSetEntity(cmProductCode = cmCode)
//
//                    product.set?.names?.forEach { (lang, name) ->
//                        val nameTranslationEntity =
//                            NameTranslationEntity(
//                                id = 0,
//                                productSet = productSetEntity,
//                                languageCode = lang.code,
//                                name = name,
//                            )
//                        productSetEntity.nameTranslations.add(nameTranslationEntity)
//                    }
//
//                    val productEntity = productMapper.toEntity(product, productSetEntity)
//                    productEntity
//                }
//            }
//
//        // Persist all entities in a single batch operation within the same transaction
//        return productJpaRepository.saveAll(entitiesToPersist)
//    }




    @Transactional
    override fun deleteAll() {
        searchResultJpaRepository.deleteAll()
    }

    override fun countByQuery(query: String): Int = searchResultJpaRepository.countByQuery(query)
}
