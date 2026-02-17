package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.repository

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.ProductEntity
import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper.ProductMapper
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.port.out.ProductDetailsRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductDetailsRepositoryAdapter(
    private val jpaRepository: ProductJpaRepository,
    private val sellOfferJpaRepository: SellOfferJpaRepository,
    private val mapper: ProductMapper
) : ProductDetailsRepository {
    
    override fun findByCmIdAndGenreAndTypeAndLangAndSetname(cmId: String, genre: String, type: String, lang: String, setname: String): Product? {
        val entity = jpaRepository.findByCmIdAndGenreAndType(cmId, genre, type)
        return entity?.let { mapper.toDomain(it) }
    }
    
    @Transactional
    override fun save(product: Product): Product {
        val entity = mapper.toEntity(product)
        val saved = jpaRepository.save(entity)
        sellOfferJpaRepository.saveAll(entity.sellOffers)
        return mapper.toDomain(saved)
    }
    
    @Transactional
    override fun saveAll(products: List<Product>): List<Product> {
        val entities = products.map { mapper.toEntity(it) }
        val productEntities = jpaRepository.saveAll(entities)
        val allSellOffers = entities.flatMap { it.sellOffers }
        sellOfferJpaRepository.saveAll(allSellOffers)
        return productEntities.map { mapper.toDomain(it) }
    }
}
