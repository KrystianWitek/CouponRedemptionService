package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.config

import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.GeoIpProviderAdapter
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GeoIpProperties::class)
internal class GeoIpConfiguration {
    private val log = KotlinLogging.logger {}

    @Bean
    fun geoIpRestClient(
        restClientBuilder: RestClient.Builder,
        properties: GeoIpProperties,
    ): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.connectTimeout)
                setReadTimeout(properties.readTimeout)
            }

        return restClientBuilder
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun geoIpCircuitBreakerRegistry(properties: GeoIpProperties): CircuitBreakerRegistry =
        CircuitBreakerRegistry.of(properties.circuitBreaker.toCircuitBreakerConfig())

    @Bean
    fun geoIpCircuitBreaker(geoIpCircuitBreakerRegistry: CircuitBreakerRegistry): CircuitBreaker =
        geoIpCircuitBreakerRegistry.circuitBreaker(GEO_IP_CIRCUIT_BREAKER_NAME).apply {
            eventPublisher.onStateTransition { event ->
                val message = {
                    "GeoIP circuit breaker state changed. " +
                        "[from: ${event.stateTransition.fromState}, to: ${event.stateTransition.toState}]"
                }
                when (event.stateTransition.toState) {
                    CircuitBreaker.State.OPEN -> log.warn(message)
                    else -> log.info(message)
                }
            }
        }

    @Bean
    fun geoIpProvider(
        restClient: RestClient,
        properties: GeoIpProperties,
        geoIpCircuitBreaker: CircuitBreaker,
    ) = GeoIpProviderAdapter(restClient, properties, geoIpCircuitBreaker)

    private companion object {
        const val GEO_IP_CIRCUIT_BREAKER_NAME = "geoIpProvider"
    }
}

private fun GeoIpProperties.CircuitBreakerProperties.toCircuitBreakerConfig(): CircuitBreakerConfig =
    CircuitBreakerConfig
        .custom()
        .slidingWindowType(slidingWindowType)
        .slidingWindowSize(slidingWindowSize)
        .minimumNumberOfCalls(minimumNumberOfCalls)
        .failureRateThreshold(failureRateThreshold)
        .waitDurationInOpenState(waitDurationInOpenState)
        .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
        .build()
