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
        ensureSuccessfulResponse()
        return parseCountryCode()
    }

    private fun ensureSuccessfulResponse() {
        if (!success) {
            throw GeoIpLookupException(message ?: "GeoIP provider could not resolve the address")
        }
    }

    private fun parseCountryCode(): CountryCode {
        val providerCountryCode =
            countryCode
                ?: throw GeoIpLookupException("GeoIP provider returned an invalid country code")

        return try {
            CountryCode.from(providerCountryCode)
        } catch (exception: IllegalArgumentException) {
            throw GeoIpLookupException(
                "GeoIP provider returned an invalid country code",
                exception,
            )
        }
    }
}
