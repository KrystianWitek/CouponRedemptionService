package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties

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
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GeoIpProperties::class)
    internal class TestConfiguration
}
