package io.github.havonte1.kartalog.backend.adapter.inbound.rest.strategy

import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.ScrapingStrategySelector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${management.endpoints.web.base-path:/actuator}/scraper")
class ScraperManagementController(
    private val selector: ScrapingStrategySelector,
) {
    data class StrategyInfo(
        val id: String,
        val displayName: String,
        val isAvailable: Boolean,
        val active: Boolean,
    )

    data class SwitchRequest(
        val strategy: String,
    )

    @GetMapping("/strategies")
    fun listStrategies(): List<StrategyInfo> =
        selector.getAll().map {
            StrategyInfo(
                id = it.id,
                displayName = it.displayName,
                isAvailable = it.isAvailable,
                active = it.id == selector.getActiveId(),
            )
        }

    @GetMapping("/strategy")
    fun getActiveStrategy(): StrategyInfo {
        val active = selector.get()
        return StrategyInfo(active.id, active.displayName, active.isAvailable, true)
    }

    @PutMapping("/strategy")
    fun switchStrategy(@RequestBody request: SwitchRequest): StrategyInfo {
        selector.switch(request.strategy)
        return getActiveStrategy()
    }
}
