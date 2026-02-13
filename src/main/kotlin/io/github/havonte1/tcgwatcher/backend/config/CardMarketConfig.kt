package io.github.havonte1.tcgwatcher.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cardmarket")
class CardMarketConfig {
    var basePath: String = "https://www.cardmarket.com"
    var timeoutMs: Long = 5000L
    var retryAttempts: Int = 3
    var circuitBreaker: CircuitBreakerConfig = CircuitBreakerConfig()
    class CircuitBreakerConfig {
        var failureThreshold: Int = 3
        var waitDurationSec: Long = 30L
        var slidingWindowSec: Long = 60L
    }
}
