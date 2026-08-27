package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.model

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
internal data class IpWhoIsResponse(
    val success: Boolean,
    val countryCode: String? = null,
    val message: String? = null,
) {
    fun toCountryCode(): CountryCode {
        if (!success) {
            throw GeoIpLookupException(message ?: "GeoIP provider could not resolve the address")
        }

        val countryCode = countryCode
            ?.trim()
            ?.takeIf { it.length == COUNTRY_CODE_LENGTH && it.all(Char::isLetter) }
            ?: throw GeoIpLookupException("GeoIP provider returned an invalid country code")

        return CountryCode.from(countryCode)
    }

    private companion object {
        const val COUNTRY_CODE_LENGTH = 2
    }
}
