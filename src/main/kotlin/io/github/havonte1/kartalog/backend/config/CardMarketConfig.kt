package io.github.havonte1.kartalog.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cardmarket")
class CardMarketConfig {
    var basePath: String = "https://www.cardmarket.com"
}
