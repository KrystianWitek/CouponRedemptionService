package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("geo-ip")
internal data class GeoIpProperties(
    val baseUrl: URI,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val excludedAddresses: Set<String> = emptySet(),
)
