package io.github.havonte1.tcgwatcher.backend.config

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer
import jakarta.ws.rs.NotFoundException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Resilience4jConfig {
    @Bean
    fun cardMarketCircuitBreakerCustomizer(): CircuitBreakerConfigCustomizer =
        CircuitBreakerConfigCustomizer.of("cardMarketCircuitBreaker") { builder ->
            builder.ignoreExceptions(NotFoundException::class.java)
        }
}
