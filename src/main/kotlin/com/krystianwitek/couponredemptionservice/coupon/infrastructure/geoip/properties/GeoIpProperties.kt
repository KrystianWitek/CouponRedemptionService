package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("geo-ip")
internal data class GeoIpProperties(
    val baseUrl: URI,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val excludedAddresses: Set<String> = emptySet(),
    val circuitBreaker: CircuitBreakerProperties = CircuitBreakerProperties(),
) {
    internal data class CircuitBreakerProperties(
        val slidingWindowType: SlidingWindowType = SlidingWindowType.COUNT_BASED,
        val slidingWindowSize: Int = 10,
        val minimumNumberOfCalls: Int = 5,
        val failureRateThreshold: Float = 50f,
        val waitDurationInOpenState: Duration = Duration.ofSeconds(10),
        val permittedNumberOfCallsInHalfOpenState: Int = 3,
    )
}
