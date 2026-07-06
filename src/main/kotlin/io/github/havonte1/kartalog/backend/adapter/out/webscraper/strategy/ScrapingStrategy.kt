package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

interface ScrapingStrategy {
    val id: String
    val displayName: String
    val isAvailable: Boolean
    suspend fun fetch(url: String): String
    fun close()
}
