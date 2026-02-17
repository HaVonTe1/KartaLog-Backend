package io.github.havonte1.tcgwatcher.backend.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class Resilience4jConfig(
    private val config: CardMarketConfig
) {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(config.circuitBreaker.failureRateThreshold.toFloat())
            .slowCallRateThreshold(config.circuitBreaker.slowCallRateThreshold.toFloat())
            .slowCallDurationThreshold(Duration.ofSeconds(config.circuitBreaker.slowCallDurationThresholdSec))
            .minimumNumberOfCalls(config.circuitBreaker.minimumNumberOfCalls)
            .slidingWindowSize(config.circuitBreaker.slidingWindowSize)
            .waitDurationInOpenState(Duration.ofSeconds(config.circuitBreaker.waitDurationInOpenStateSec))
            .permittedNumberOfCallsInHalfOpenState(config.circuitBreaker.permittedNumberOfCallsInHalfOpenState)
            .automaticTransitionFromOpenToHalfOpenEnabled(config.circuitBreaker.automaticTransitionFromOpenToHalfOpenEnabled)
            .build()

        return CircuitBreakerRegistry.of(circuitBreakerConfig)
    }

    @Bean
    fun retryRegistry(): RetryRegistry {
        val retryConfig = RetryConfig.custom<String>()
            .maxAttempts(config.retry.maxAttempts)
            .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                Duration.ofSeconds(config.retry.waitDurationSec),
                config.retry.waitDurationMultiplier
            ))
            .build()

        return RetryRegistry.of(retryConfig)
    }
}
