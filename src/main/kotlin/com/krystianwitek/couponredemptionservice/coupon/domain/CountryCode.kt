package com.krystianwitek.couponredemptionservice.coupon.domain

import java.util.Locale

@JvmInline
value class CountryCode private constructor(
	val value: String,
) {
	companion object {
		private val iso3166Alpha2Pattern = Regex("[A-Z]{2}")

		fun from(value: String): CountryCode {
			val normalizedValue = value.trim().uppercase(Locale.ROOT)
			require(iso3166Alpha2Pattern.matches(normalizedValue)) {
				"Country code must use the ISO 3166-1 alpha-2 format"
			}

			return CountryCode(normalizedValue)
		}
	}
}
