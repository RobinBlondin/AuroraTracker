package com.example.auroratracker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@ConfigurationProperties("aurora.points")
data class AuroraPointsProperties(
    val cacheTtl: Duration = Duration.ofSeconds(60),
    val requestsPerMinute: Int = 60,
    val maxTrackedClients: Int = 10_000
) {
    init {
        require(cacheTtl >= Duration.ofSeconds(1)) { "Aurora points cache TTL must be at least one second" }
        require(requestsPerMinute > 0) { "Aurora points request limit must be positive" }
        require(maxTrackedClients > 0) { "Aurora points client limit must be positive" }
    }
}

@Configuration
@EnableConfigurationProperties(AuroraPointsProperties::class)
class AuroraPointsConfig {
    @Bean
    fun auroraPointsClock(): Clock = Clock.systemUTC()
}
