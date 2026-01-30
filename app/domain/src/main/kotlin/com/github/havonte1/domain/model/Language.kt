package com.github.havonte1.domain.model

/**
 * Core domain model representing an ISO language code.
 * This is a plain Kotlin data class without any persistence annotations.
 */
data class Language(
    val code: String,
    val name: String? = null
)
