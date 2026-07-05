package io.github.havonte1.kartalog.backend.adapter.out.persistence.repository

import io.github.havonte1.kartalog.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.kartalog.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.kartalog.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.kartalog.backend.adapter.out.persistence.entity.SellOfferEntity
import io.github.havonte1.kartalog.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.kartalog.backend.domain.model.Product
import io.github.havonte1.kartalog.backend.domain.port.out.ProductRepository
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

            val existingSet = productSetJpaRepository.findByCmProductCode(cmSetCode)
            logger.debug { "Existing set for $cmSetCode: ${existingSet}" }

            val productSetEntity =
                if (existingSet!=null) {
                    existingSet
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
        val uniqueCmSetCodes = products.mapNotNull { it.set?.cmCode }.distinct()
        logger.debug { "saveAll() called with ${products.size} products, unique cmCodes: $uniqueCmSetCodes" }

        val existingSets = productSetJpaRepository.findByCmProductCodeIn(uniqueCmSetCodes)
        val existingSetMap = existingSets.associateBy { it.cmProductCode }
        logger.debug { "Found ${existingSets.size} existing sets" }

        val newCmCodes = uniqueCmSetCodes.filter { it !in existingSetMap.keys }
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

        val externalIds = products.map { it.externalId }
        val existingProductMap = productJpaRepository
            .findAllWithDetailsByExternalIdIn(externalIds)
            .associateBy { it.externalId }

        val (toSave, unchanged) = products.partition { product ->
            val existing = existingProductMap[product.externalId]
            existing == null ||
                (mapper.toEntity(product, allSetMap[product.set?.cmCode ?: "dummy"]!!).compareTo(existing) != 0)
        }

        logger.debug { "${toSave.size} products changed, ${unchanged.size} unchanged – skipping unchanged" }
        val productEntitiesToSave = toSave.map { product ->

            if(existingProductMap.containsKey(product.externalId)) {
                //changed --> update
                logger.debug {" update for ${product.externalId} : new price: ${product.price} "}
                existingProductMap[product.externalId]!!.copy(price = product.price)
            } else {
                //new --> insert
                logger.debug { " new product: $product" }
                val setEntity = allSetMap[product.set?.cmCode ?: "dummy"]!!
                mapper.toEntity(product, setEntity)
            }

        }
        val savedProducts = productJpaRepository.saveAll(productEntitiesToSave)
        productJpaRepository.flush()    // IDs materialisieren, bevor SellOffers referenzieren

        updateSellOffers(savedProducts, products)

        val unchangedEntities = unchanged.mapNotNull { existingProductMap[it.externalId] }
        updateSellOffers(unchangedEntities, unchanged)

        return (savedProducts + unchangedEntities).map { mapper.toDomain(it) }
    }


    /**
     * Synchronisiert SellOffers für eine Liste von (bereits gespeicherten) ProductEntities.
     *
     * – Neue Offers werden eingefügt.
     * – Verschwundene Offers werden gelöscht.
     * – Unveränderte Offers bleiben unangetastet (kein unnötiges UPDATE).
     */
    private fun updateSellOffers(
        savedEntities: List<ProductEntity>,
        domainProducts: List<Product>,
    ) {
        if (savedEntities.isEmpty()) return

        val domainByExternalId = domainProducts.associateBy { it.externalId }

        // Alle aktuellen SellOffers dieser Products in einem Query laden
        val productIds = savedEntities.mapNotNull { it.id }
        val existingOffersByProductId: Map<Long, List<SellOfferEntity>> =
            sellOfferJpaRepository.findAllByProductIdIn(productIds)
                .groupBy { it.product.id!! }

        val toInsert = mutableListOf<SellOfferEntity>()
        val toDelete = mutableListOf<SellOfferEntity>()

        for (productEntity in savedEntities) {
            val domain = domainByExternalId[productEntity.externalId] ?: continue
            val desiredOffers = domain.sellOffers
                ?.map { mapper.toSellOfferEntity(it, productEntity) }   ?: emptyList()
            val existingOffers = existingOffersByProductId[productEntity.id!!] ?: emptyList()

            // Neu: in desired, aber nicht in existing
            val newOffers = desiredOffers.filter { desired ->
                existingOffers.none { ex -> ex.businessEquals(desired) }
            }
            // Veraltet: in existing, aber nicht mehr in desired
            val obsoleteOffers = existingOffers.filter { ex ->
                desiredOffers.none { desired -> ex.businessEquals(desired) }
            }

            toInsert += newOffers
            toDelete += obsoleteOffers
        }

        if (toDelete.isNotEmpty()) {
            sellOfferJpaRepository.deleteAll(toDelete)
            logger.debug { "Deleted ${toDelete.size} obsolete SellOfferEntities" }
        }
        if (toInsert.isNotEmpty()) {
            sellOfferJpaRepository.saveAll(toInsert)
            logger.debug { "Inserted ${toInsert.size} new SellOfferEntities" }
        }
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Product? = productJpaRepository.findWithDetailsById(id)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findByExternalId(externalId: Long): Product? =
        productJpaRepository.findByExternalId(externalId)?.let { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAll(): List<Product> = productJpaRepository.findAllWithDetails().map { mapper.toDomain(it) }

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
