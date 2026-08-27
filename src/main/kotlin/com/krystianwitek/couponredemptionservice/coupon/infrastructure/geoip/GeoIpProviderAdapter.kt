package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import org.springframework.web.client.RestClient

internal class GeoIpProviderAdapter(
    private val restClientBuilder: RestClient.Builder,
) : GeoIpProvider {
    override fun resolveCountry(ipAddress: String): CountryCode = TODO("Needs implementation")
}
