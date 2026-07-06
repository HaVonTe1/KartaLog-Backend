package io.github.havonte1.kartalog.backend.config

import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.CamoufoxPlaywrightStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.CamoufoxPythonWorkerStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.ChromiumPlaywrightStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.PlaywrightExtraWorkerStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.PuppeteerWorkerStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.ScrapingStrategyRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ScrapingStrategyConfig(
    @Value("\${playwright.chromium.executable-path:}") private val chromiumExe: String,
    @Value("\${playwright.camoufox.executable-path:}") private val camoufoxExe: String,
    @Value("\${scraper.workers.puppeteer.url:}") private val puppeteerUrl: String,
    @Value("\${scraper.workers.playwright-extra.url:}") private val playwrightExtraUrl: String,
    @Value("\${scraper.workers.camoufox-python.url:}") private val camoufoxPythonUrl: String,
) {
    @Bean
    fun chromiumStrategy(): ChromiumPlaywrightStrategy =
        ChromiumPlaywrightStrategy(
            executablePath = chromiumExe.takeIf { it.isNotBlank() }
        )

    @Bean
    fun camoufoxStrategy(): CamoufoxPlaywrightStrategy =
        CamoufoxPlaywrightStrategy(
            executablePath = camoufoxExe.takeIf { it.isNotBlank() }
        )

    @Bean
    fun puppeteerWorkerStrategy(): PuppeteerWorkerStrategy = PuppeteerWorkerStrategy(puppeteerUrl)

    @Bean
    fun playwrightExtraWorkerStrategy(): PlaywrightExtraWorkerStrategy =
        PlaywrightExtraWorkerStrategy(playwrightExtraUrl)

    @Bean
    fun camoufoxPythonWorkerStrategy(): CamoufoxPythonWorkerStrategy =
        CamoufoxPythonWorkerStrategy(camoufoxPythonUrl)

    @Bean
    fun strategyRegistry(
        chromiumStrategy: ChromiumPlaywrightStrategy,
        camoufoxStrategy: CamoufoxPlaywrightStrategy,
        puppeteerWorkerStrategy: PuppeteerWorkerStrategy,
        playwrightExtraWorkerStrategy: PlaywrightExtraWorkerStrategy,
        camoufoxPythonWorkerStrategy: CamoufoxPythonWorkerStrategy,
    ): ScrapingStrategyRegistry =
        ScrapingStrategyRegistry(
            listOf(
                chromiumStrategy,
                camoufoxStrategy,
                puppeteerWorkerStrategy,
                playwrightExtraWorkerStrategy,
                camoufoxPythonWorkerStrategy,
            )
        )
}
