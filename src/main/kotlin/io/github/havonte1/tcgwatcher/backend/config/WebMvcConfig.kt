package io.github.havonte1.tcgwatcher.backend.config

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val rateLimiterRegistry: RateLimiterRegistry
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RateLimitingInterceptor(rateLimiterRegistry))
            .addPathPatterns("/collectables/")
    }
}
