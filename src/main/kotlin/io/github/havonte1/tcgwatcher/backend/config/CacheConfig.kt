package io.github.havonte1.tcgwatcher.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cache")
class CacheConfig {
    /** TTL in hours for cached search results. Default: 1 hour */
    var ttlHours: Long = 1L
}
