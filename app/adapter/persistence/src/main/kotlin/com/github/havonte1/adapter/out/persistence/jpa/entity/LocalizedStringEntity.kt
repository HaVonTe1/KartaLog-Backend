package com.github.havonte1.adapter.out.persistence.entity

import com.github.havonte1.domain.model.LocalizedString
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * JPA entity for translatable strings linked to a product.
 * Mirrors the core domain model [com.github.havonte1.domain.model.LocalizedString].
 */
@Entity
@Table(name = "localized_strings")
data class LocalizedStringEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_code", referencedColumnName = "code", nullable = false)
    val language: LanguageEntity,

    @Column(name = "type", length = 30, nullable = false)
    val type: String,

    @Column(name = "value", columnDefinition = "TEXT", nullable = false)
    val value: String
) {
    fun toDomain() = LocalizedString(
        id = id,
        product = product.toDomain(),
        language = language.toDomain(),
        type = type,
        value = value
    )
}
