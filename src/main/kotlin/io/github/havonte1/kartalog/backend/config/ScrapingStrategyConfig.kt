package io.github.havonte1.kartalog.backend.config

import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.CamoufoxCdpStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.CamoufoxPlaywrightStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.CamoufoxPythonWorkerStrategy
import io.github.havonte1.kartalog.backend.adapter.out.webscraper.strategy.ChromeCdpStrategy
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
    @Value("\${scraper.chrome-cdp.url:}") private val chromeCdpUrl: String,
    @Value("\${scraper.chrome-cdp.pool-size:3}") private val chromeCdpPoolSize: Int,
    @Value("\${scraper.chrome-cdp.max-concurrent:5}") private val chromeCdpMaxConcurrent: Int,
    @Value("\${scraper.camoufox-cdp.url:}") private val camoufoxCdpUrl: String,
    @Value("\${scraper.camoufox-cdp.health-url:}") private val camoufoxCdpHealthUrl: String,
    @Value("\${scraper.camoufox-cdp.pool-size:3}") private val camoufoxCdpPoolSize: Int,
    @Value("\${scraper.camoufox-cdp.max-concurrent:3}") private val camoufoxCdpMaxConcurrent: Int,
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
    fun chromeCdpStrategy(): ChromeCdpStrategy =
        ChromeCdpStrategy(
            cdpUrl = chromeCdpUrl,
            poolSize = chromeCdpPoolSize,
            maxConcurrent = chromeCdpMaxConcurrent,
        )

    @Bean
    fun camoufoxCdpStrategy(): CamoufoxCdpStrategy =
        CamoufoxCdpStrategy(
            wsUrl = camoufoxCdpUrl,
            healthUrl = camoufoxCdpHealthUrl,
            poolSize = camoufoxCdpPoolSize,
            maxConcurrent = camoufoxCdpMaxConcurrent,
        )

    @Bean
    fun strategyRegistry(
        chromiumStrategy: ChromiumPlaywrightStrategy,
        camoufoxStrategy: CamoufoxPlaywrightStrategy,
        puppeteerWorkerStrategy: PuppeteerWorkerStrategy,
        playwrightExtraWorkerStrategy: PlaywrightExtraWorkerStrategy,
        camoufoxPythonWorkerStrategy: CamoufoxPythonWorkerStrategy,
        chromeCdpStrategy: ChromeCdpStrategy,
        camoufoxCdpStrategy: CamoufoxCdpStrategy,
    ): ScrapingStrategyRegistry =
        ScrapingStrategyRegistry(
            listOf(
                chromiumStrategy,
                camoufoxStrategy,
                puppeteerWorkerStrategy,
                playwrightExtraWorkerStrategy,
                camoufoxPythonWorkerStrategy,
                chromeCdpStrategy,
                camoufoxCdpStrategy,
            )
        )
}
