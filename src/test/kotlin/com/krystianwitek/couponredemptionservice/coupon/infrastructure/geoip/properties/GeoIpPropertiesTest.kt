package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.time.Duration

class GeoIpPropertiesTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun `should bind GeoIP properties from application yaml`() {
        // given
        val contextRunner =
            contextRunner.withPropertyValues(
                "GEO_IP_BASE_URL=https://ipwho.is",
                "GEO_IP_CONNECT_TIMEOUT=1s",
                "GEO_IP_READ_TIMEOUT=2s",
                "GEO_IP_EXCLUDED_ADDRESSES=127.0.0.1,::1",
            )

        // when
        contextRunner.run { context ->
            // then
            assertThat(context).hasNotFailed()

            val properties = context.getBean(GeoIpProperties::class.java)
            assertThat(properties.baseUrl).isEqualTo(URI.create("https://ipwho.is"))
            assertThat(properties.connectTimeout).isEqualTo(Duration.ofSeconds(1))
            assertThat(properties.readTimeout).isEqualTo(Duration.ofSeconds(2))
            assertThat(properties.excludedAddresses)
                .containsExactlyInAnyOrder("127.0.0.1", "::1")

            val circuitBreaker = properties.circuitBreaker
            assertThat(circuitBreaker.slidingWindowType).isEqualTo(SlidingWindowType.COUNT_BASED)
            assertThat(circuitBreaker.slidingWindowSize).isEqualTo(10)
            assertThat(circuitBreaker.minimumNumberOfCalls).isEqualTo(5)
            assertThat(circuitBreaker.failureRateThreshold).isEqualTo(50f)
            assertThat(circuitBreaker.waitDurationInOpenState).isEqualTo(Duration.ofSeconds(10))
            assertThat(circuitBreaker.permittedNumberOfCallsInHalfOpenState).isEqualTo(3)
        }
    }

    @Test
    fun `should bind circuit breaker override from properties`() {
        // given
        val contextRunner =
            contextRunner.withPropertyValues(
                "GEO_IP_BASE_URL=https://ipwho.is",
                "GEO_IP_CONNECT_TIMEOUT=1s",
                "GEO_IP_READ_TIMEOUT=2s",
                "GEO_IP_EXCLUDED_ADDRESSES=127.0.0.1,::1",
                "geo-ip.circuit-breaker.sliding-window-size=20",
            )

        // when
        contextRunner.run { context ->
            // then
            assertThat(context).hasNotFailed()

            val circuitBreaker = context.getBean(GeoIpProperties::class.java).circuitBreaker
            assertThat(circuitBreaker.slidingWindowSize).isEqualTo(20)
            assertThat(circuitBreaker.minimumNumberOfCalls).isEqualTo(5)
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GeoIpProperties::class)
    internal class TestConfiguration
}
