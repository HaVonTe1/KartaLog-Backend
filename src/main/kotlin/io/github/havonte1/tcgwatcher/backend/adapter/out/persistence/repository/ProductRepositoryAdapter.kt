package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.NameTranslationEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductSetEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Implementation of the outbound port [ProductRepository] using Spring Data JPA.
 */
@Component
class ProductRepositoryAdapter(
    private val jpaRepository: ProductJpaRepository,
    private val sellOfferJpaRepository: SellOfferJpaRepository,
    private val mapper: ProductMapper
) : ProductRepository {
    override fun deleteAll() {
        jpaRepository.deleteAll()
    }


    @Transactional
    override fun save(product: Product): Product {
        val productSetEntity = ProductSetEntity(id = 0, cmProductCode = product.set?.cmCode ?: "dummy")
        val nameTranslationEntity = NameTranslationEntity(
            id = 0,
            productSet = productSetEntity,
            languageCode = "de",
            name = product.set?.names["de"] ?: "dummy"
        )
        productSetEntity.nameTranslations.add(nameTranslationEntity)
        val entity = mapper.toEntity(product, productSetEntity)
        val saved = jpaRepository.save(entity)
        sellOfferJpaRepository.saveAll(entity.sellOffers)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun saveAll(products: List<Product>): List<Product> {
        val entities = products.map { product ->
            val productSetEntity = ProductSetEntity(id = 0, cmProductCode = product.set?.cmCode ?: "dummy")
            val nameTranslationEntity = NameTranslationEntity(
                id = 0,
                productSet = productSetEntity,
                languageCode = "de",
                name = product.set?.names["de"] ?: "dummy"
            )
            productSetEntity.nameTranslations.add(nameTranslationEntity)
            mapper.toEntity(product, productSetEntity)
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
