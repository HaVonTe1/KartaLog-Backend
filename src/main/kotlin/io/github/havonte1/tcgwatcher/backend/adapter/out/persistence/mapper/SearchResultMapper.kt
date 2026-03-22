package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.mapper

import io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity.SearchResultEntity
import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SearchResult
import org.springframework.stereotype.Component

/**
 * Mapper between domain [SearchResult] and JPA [SearchResultEntity].
 * Delegates product conversion to [ProductMapper].
 */
@Component
class SearchResultMapper(
    private val productMapper: ProductMapper,
) {
    fun toDomain(entity: SearchResultEntity): SearchResult {
        val products: List<Product> = entity.products.map { productMapper.toDomain(it) }
        return SearchResult(
            id = entity.id,
            query = entity.query,
            products = products,
            cachedAt = entity.cachedAt,
        )
    }

    fun toEntityWithoutProducts(searchResult: SearchResult) =
        SearchResultEntity(
            id = searchResult.id,
            query = searchResult.query,
            cachedAt = searchResult.cachedAt,
            products = mutableSetOf(),
        )
}
