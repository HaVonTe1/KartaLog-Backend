package io.github.havonte1.tcgwatcher.backend.adapter.out.persistence.entity

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
import java.io.Serializable
import java.time.Instant

/**
 * JPA entity representing a product (collectible card).
 * Mirrors the core domain model [com.github.havonte1.domain.model.Product].
 */
@Entity
@Table(name = "products", schema = "watcher")
data class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "external_id", nullable = false, unique = true)
    val externalId: Long,

    @Column(name = "set_name")
    val setName: String? = null,

    @Column(name = "rarity")
    val rarity: String? = null,

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
        fetch = FetchType.EAGER
    )
    val nameTranslations: MutableSet<NameTranslationEntity> = mutableSetOf()
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    // JPA requires a no‑arg constructor
    constructor() : this(0, 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProductEntity) return false
        // If both have DB ids, compare by id; otherwise fall back to externalId
        if (id != null && other.id != null) return id == other.id
        return externalId == other.externalId
    }

    fun compareTo(other: ProductEntity): Int {
        return compareValuesBy(this, other, { it.price }, { it.priceTrend }, { it.priceTrendValid })
    }

    override fun hashCode(): Int = id?.hashCode() ?: externalId.hashCode()
}
