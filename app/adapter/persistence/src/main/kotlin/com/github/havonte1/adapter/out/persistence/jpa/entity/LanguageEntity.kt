package com.github.havonte1.adapter.out.persistence.entity

import com.github.havonte1.domain.model.Language
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * JPA entity for language codes. Mirrors the core [com.github.havonte1.domain.model.Language] model.
 */
@Entity
@Table(name = "languages")
data class LanguageEntity(
    @Id
    @Column(length = 5)
    val code: String,
    val name: String? = null
) {
    fun toDomain() = Language(code = code, name = name)
}
