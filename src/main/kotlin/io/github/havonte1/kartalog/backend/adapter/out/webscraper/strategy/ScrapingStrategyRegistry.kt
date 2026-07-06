package io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy

class ScrapingStrategyRegistry(
    strategies: List<ScrapingStrategy>,
) {
    private val index = strategies.associateBy { it.id }

    fun get(id: String): ScrapingStrategy? = index[id]

    fun getAll(): Collection<ScrapingStrategy> = index.values
}
