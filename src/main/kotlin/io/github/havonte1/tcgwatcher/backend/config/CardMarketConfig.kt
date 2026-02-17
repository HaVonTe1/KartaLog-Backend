package io.github.havonte1.tcgwatcher.backend.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cardmarket")
class CardMarketConfig {
    var basePath: String = "https://www.cardmarket.com"
    var timeoutMs: Long = 5000L
    var retryAttempts: Int = 3
    var circuitBreaker: CircuitBreakerConfig = CircuitBreakerConfig()
    var retry: RetryConfig = RetryConfig()

    class CircuitBreakerConfig {
        var failureRateThreshold: Int = 50
        var slowCallRateThreshold: Int = 50
        var slowCallDurationThresholdSec: Long = 10
        var minimumNumberOfCalls: Int = 10
        var slidingWindowSize: Int = 60
        var waitDurationInOpenStateSec: Long = 30
        var permittedNumberOfCallsInHalfOpenState: Int = 3
        var automaticTransitionFromOpenToHalfOpenEnabled: Boolean = true
    }

    class RetryConfig {
        var maxAttempts: Int = 3
        var waitDurationSec: Long = 1
        var waitDurationMultiplier: Double = 2.0
    }
}
