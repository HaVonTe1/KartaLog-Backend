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
    private val jpaRepository: ProductJpaRepository,
    private val sellOfferJpaRepository: SellOfferJpaRepository,
    private val productSetJpaRepository: ProductSetJpaRepository,
    private val mapper: ProductMapper
) : ProductRepository {
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }

    @Transactional
    override fun save(product: Product): Product {
        val cmCode = product.set?.cmCode ?: "dummy"
        logger.debug { "save() called with cmCode: $cmCode, product.set: ${product.set}" }
        
        val existingSets = productSetJpaRepository.findByCmProductCode(cmCode)
        logger.debug { "Existing sets for $cmCode: ${existingSets.size}" }
        
        val productSetEntity = if (existingSets.isNotEmpty()) {
            existingSets.first()
        } else {
            val newSet = ProductSetEntity(cmProductCode = cmCode)
            logger.debug { "Creating new ProductSetEntity with cmCode: $cmCode" }
            val nameTranslationEntity = NameTranslationEntity(
                productSet = newSet,
                languageCode = "de",
                name = product.set?.names["de"] ?: "dummy"
            )
            newSet.nameTranslations.add(nameTranslationEntity)
            val saved = productSetJpaRepository.save(newSet)
            productSetJpaRepository.flush()
            logger.debug { "Saved new ProductSetEntity with id: ${saved.id}" }
            saved
        }

        logger.debug { "Using ProductSetEntity with id: ${productSetEntity.id} for product" }
        
        val entity = mapper.toEntity(product, productSetEntity)
        logger.debug { "Created ProductEntity with setId: ${entity.setId}" }
        
        val saved = jpaRepository.save(entity)
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
        
        val newSets = newCmCodes.map { cmCode ->
            val product = products.first { it.set?.cmCode == cmCode }
            val productSetEntity = ProductSetEntity(cmProductCode = cmCode)
            val nameTranslationEntity = NameTranslationEntity(
                productSet = productSetEntity,
                languageCode = "de",
                name = product.set?.names["de"] ?: "dummy"
            )
            productSetEntity.nameTranslations.add(nameTranslationEntity)
            productSetEntity
        }

        val savedNewSets = if (newSets.isNotEmpty()) {
            val saved = productSetJpaRepository.saveAll(newSets)
            productSetJpaRepository.flush()
            logger.debug { "Saved ${saved.size} new ProductSetEntities with ids: ${saved.map { it.id }}" }
            saved
        } else {
            emptyList()
        }

        val allSetMap = (existingSets + savedNewSets).associateBy { it.cmProductCode }

        val entities = products.map { product ->
            val cmCode = product.set?.cmCode ?: "dummy"
            val productSetEntity = allSetMap[cmCode]!!
            logger.debug { "Mapping product ${product.externalId} with ProductSetEntity id: ${productSetEntity.id}" }
            mapper.toEntity(product, productSetEntity)
        }
        
        entities.forEach { entity ->
            logger.debug { "ProductEntity setId before save: ${entity.setId}" }
        }
        
        val productEntities = jpaRepository.saveAll(entities)
        val allSellOffers = entities.flatMap { it.sellOffers }
        sellOfferJpaRepository.saveAll(allSellOffers)
        return productEntities.map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Product? =
        jpaRepository.findById(id).orElse(null)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findByExternalId(externalId: Long): Product? =
        jpaRepository.findByExternalId(externalId)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAll(): List<Product> =
        jpaRepository.findAll().map { mapper.toDomain(it) }

    @Transactional
    override fun delete(product: Product) {
        if (product.id != null) {
            jpaRepository.findById(product.id).ifPresent { jpaRepository.delete(it) }
        }
    }
    @Transactional(readOnly = true)
    override fun findByCmId(
        cmId: String,
    ): Product? {
        val entity = jpaRepository.findByCmId(cmId)
        return entity?.let { mapper.toDomain(it) }
    }

}
