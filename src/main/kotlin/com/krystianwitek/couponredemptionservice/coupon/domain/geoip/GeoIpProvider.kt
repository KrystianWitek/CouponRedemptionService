package com.krystianwitek.couponredemptionservice.coupon.domain.geoip

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode

interface GeoIpProvider {
    fun resolveCountry(ipAddress: String): CountryCode
}

class GeoIpLookupException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
