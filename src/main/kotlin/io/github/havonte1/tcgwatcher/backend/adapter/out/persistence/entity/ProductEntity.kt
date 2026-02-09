package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.Instant

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

    @Column(name = "code_info")
    val codeInfo: String? = null,

    @Column(name = "code_info_valid")
    val codeInfoValid: Boolean? = null,

    @Column(name = "genre")
    val genre: String? = null,

    @Column(name = "type")
    val type: String? = null,

    @Column(name = "cm_id")
    val cmId: String? = null,

    @Column(name = "cm_link")
    val cmLink: String? = null,

    @Column(name = "img_link")
    val imgLink: String? = null,

    @Column(name = "price")
    val price: String? = null,

    @Column(name = "price_trend")
    val priceTrend: String? = null,

    @Column(name = "price_trend_valid")
    val priceTrendValid: Boolean? = null,

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
    val nameTranslations: MutableSet<NameTranslationEntity> = mutableSetOf()
): Serializable {
    // JPA requires a no‑arg constructor
    constructor(): this(0, 0)
}
