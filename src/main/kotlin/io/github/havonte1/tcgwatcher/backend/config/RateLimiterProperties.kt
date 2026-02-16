package io.github.havonte1.tcgwatcher.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
class RateLimiterProperties {
    var enabled: Boolean = true
    var requestsPerPeriod: Int = 100
    var timeoutDurationSec: Long = 60L
}
