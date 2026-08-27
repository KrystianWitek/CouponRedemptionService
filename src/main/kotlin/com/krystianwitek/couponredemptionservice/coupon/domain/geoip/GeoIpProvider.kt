package com.krystianwitek.couponredemptionservice.coupon.domain.geoip

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode

interface GeoIpProvider {
    fun resolveCountry(ipAddress: String): CountryCode
}
