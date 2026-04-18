package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
class ProductRepositoryAdapter(
    private val productJpaRepository: ProductJpaRepository,
    private val sellOfferJpaRepository: SellOfferJpaRepository,
    private val productSetJpaRepository: ProductSetJpaRepository,
    private val mapper: ProductMapper,
) : ProductRepository {
    override fun deleteAll() {
        productJpaRepository.deleteAll()
    }

    @Transactional
    override fun save(product: Product): Product {
        logger.debug { "Saving new product: $product" }

        val productSetEntity: ProductSetEntity = if (product.set != null) {
            val cmSetCode = product.set.cmCode

            val existingSets = productSetJpaRepository.findByCmProductCode(cmSetCode)
            logger.debug { "Existing sets for $cmSetCode: ${existingSets.size}" }

            val productSetEntity =
                if (existingSets.isNotEmpty()) {
                    existingSets.first()
                } else {
                    val newSet = ProductSetEntity(cmProductCode = cmSetCode)
                    logger.debug { "Creating new ProductSetEntity with cmCode: $cmSetCode" }
                    product.set.names.forEach { (locale, text) ->

                        val nameTranslationEntity =
                            NameTranslationEntity(
                                productSet = newSet,
                                languageCode = locale.code,
                                name = text,
                            )
                        newSet.nameTranslations.add(nameTranslationEntity)
                    }

                    val saved = productSetJpaRepository.save(newSet)
                    productSetJpaRepository.flush()
                    logger.debug { "Saved new ProductSetEntity with id: ${saved.id}" }
                    saved
                }
            productSetEntity
        } else {
            // find a way for a dummy set which can be overriden later
            ProductSetEntity(id = 0, cmProductCode = "dummy")
        }
        logger.debug { "Using ProductSetEntity with id: ${productSetEntity.id} for product" }

        val entity = mapper.toEntity(product, productSetEntity)
        logger.debug { "Created ProductEntity with setId: ${entity.setId}" }

        val saved = productJpaRepository.save(entity)
        logger.debug { "Saved ProductEntity with id: ${saved.id}, setId: ${saved.setId}" }

        sellOfferJpaRepository.saveAll(entity.sellOffers)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun saveAll(products: List<Product>): List<Product> {
        val uniqueCmCodes = products.mapNotNull { it.set?.cmCode }.distinct()
        logger.debug { "saveAll() called with ${products.size} products, unique cmCodes: $uniqueCmCodes" }

        val existingSets = uniqueCmCodes.flatMap { productSetJpaRepository.findByCmProductCode(it) }
        val existingSetMap = existingSets.associateBy { it.cmProductCode }
        logger.debug { "Found ${existingSets.size} existing sets" }

        val newCmCodes = uniqueCmCodes.filter { it !in existingSetMap.keys }
        logger.debug { "Need to create ${newCmCodes.size} new sets: $newCmCodes" }

        val newSets =
            newCmCodes.map { cmCode ->
                val product = products.first { it.set?.cmCode == cmCode }
                val productSetEntity = ProductSetEntity(cmProductCode = cmCode)
                product.set?.names?.forEach { (locale, text) ->
                    val nameTranslationEntity =
                        NameTranslationEntity(
                            productSet = productSetEntity,
                            languageCode = locale.code,
                            name = text,
                        )
                    productSetEntity.nameTranslations.add(nameTranslationEntity)
                }
                productSetEntity
            }

        val savedNewSets =
            if (newSets.isNotEmpty()) {
                val saved = productSetJpaRepository.saveAll(newSets)
                productSetJpaRepository.flush()
                logger.debug { "Saved ${saved.size} new ProductSetEntities with ids: ${saved.map { it.id }}" }
                saved
            } else {
                emptyList()
            }

        val allSetMap = (existingSets + savedNewSets).associateBy { it.cmProductCode }

        val entities =
            products.map { product ->
                val cmCode = product.set?.cmCode ?: "dummy"
                val productSetEntity = allSetMap[cmCode]!!
                logger.debug { "Mapping product ${product.externalId} with ProductSetEntity id: ${productSetEntity.id}" }
                mapper.toEntity(product, productSetEntity)
            }

        entities.forEach { entity ->
            logger.debug { "ProductEntity setId before save: ${entity.setId}" }
        }

        val productEntities = productJpaRepository.saveAll(entities)
        val allSellOffers = entities.flatMap { it.sellOffers }
        sellOfferJpaRepository.saveAll(allSellOffers)
        return productEntities.map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Product? = productJpaRepository.findById(id).orElse(null)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findByExternalId(externalId: Long): Product? =
        productJpaRepository.findByExternalId(externalId)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAll(): List<Product> = productJpaRepository.findAll().map { mapper.toDomain(it) }

    @Transactional
    override fun delete(product: Product) {
        if (product.id != null) {
            productJpaRepository.findById(product.id).ifPresent { productJpaRepository.delete(it) }
        }
    }

    @Transactional(readOnly = true)
    override fun findByCmId(cmId: String): Product? {
        val entity = productJpaRepository.findByCmId(cmId)
        return entity?.let { mapper.toDomain(it) }
    }
}
