package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.time.Duration

@ConfigurationProperties("geo-ip")
internal data class GeoIpProperties(
    val baseUrl: URI,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val excludedAddresses: Set<String> = emptySet(),
)

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GeoIpProperties::class)
internal class GeoIpConfiguration
