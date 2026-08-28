package com.krystianwitek.couponredemptionservice.coupon.domain

import java.util.Locale

@JvmInline
value class CountryCode private constructor(
    val value: String,
) {
    companion object {
        private val isoCountryCodes = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2)
        private val countryCodePattern = Regex("[A-Z]{2}")

        fun from(value: String): CountryCode {
            val normalizedCode = normalize(value)

            requireValidFormat(normalizedCode)
            requireSupportedCountry(normalizedCode)

            return CountryCode(normalizedCode)
        }

        private fun normalize(value: String): String = value.trim().uppercase(Locale.ROOT)

        private fun requireValidFormat(value: String) {
            require(countryCodePattern.matches(value)) {
                "Country code must contain exactly two letters"
            }
        }

        private fun requireSupportedCountry(value: String) {
            require(value in isoCountryCodes) {
                "Unsupported country code: $value"
            }
        }
    }
}
