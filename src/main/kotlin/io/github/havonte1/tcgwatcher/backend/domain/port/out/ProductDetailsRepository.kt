package io.github.havonte1.tcgwatcher.backend.domain.port.out

import io.github.havonte1.tcgwatcher.backend.domain.model.Product
import io.github.havonte1.tcgwatcher.backend.domain.model.SellOffer

interface ProductDetailsRepository {
    fun findByCmIdAndGenreAndTypeAndLangAndSetname(cmId: String, genre: String, type: String, lang: String, setname: String): Product?
    
    fun save(product: Product): Product
    
    fun saveAll(products: List<Product>): List<Product>
}
