package io.github.havonte1.tcgwatcher.backend.config

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RateLimiterConfiguration {

    @Bean
    fun rateLimiterProperties(): RateLimiterProperties {
        return RateLimiterProperties()
    }

    @Bean
    fun resilienceRateLimiterConfig(rateLimiterProperties: RateLimiterProperties): RateLimiterConfig {
        return RateLimiterConfig.custom()
            .limitForPeriod(rateLimiterProperties.requestsPerPeriod)
            .limitRefreshPeriod(Duration.ofSeconds(rateLimiterProperties.timeoutDurationSec))
            .timeoutDuration(Duration.ZERO)
            .build()
    }

    @Bean
    fun rateLimiter(resilienceRateLimiterConfig: RateLimiterConfig): RateLimiter {
        return RateLimiter.of("collectablesApi", resilienceRateLimiterConfig)
    }
}
