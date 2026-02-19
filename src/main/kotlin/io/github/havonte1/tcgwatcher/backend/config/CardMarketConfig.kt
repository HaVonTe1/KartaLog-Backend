package io.github.havonte1.tcgwatcher.backend.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cardmarket")
class CardMarketConfig {
    var basePath: String = "https://www.cardmarket.com"
}
