package com.github.havonte1.adapter.out.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import com.github.havonte1.domain.model.Product

/**
 * JPA entity representing a product (collectible card).
 * Mirrors the core domain model [com.github.havonte1.domain.model.Product].
 */
@Entity
@Table(name = "products")
data class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "external_id", nullable = false, unique = true)
    val externalId: Long,

    @Column(name = "set_name")
    val setName: String? = null,

    @Column(name = "rarity")
    val rarity: String? = null,

    @Column(name = "image_url")
    val imageUrl: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,

    @OneToMany(
        mappedBy = "product",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val localizedStrings: MutableSet<LocalizedStringEntity> = mutableSetOf()
) {
    fun toDomain(): Product {
        val domainLocalized = localizedStrings.map { it.toDomain() }.toMutableSet()
        return Product(
            id = id,
            externalId = externalId,
            setName = setName,
            rarity = rarity,
            imageUrl = imageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt,
            localizedStrings = domainLocalized
        )
    }
}
