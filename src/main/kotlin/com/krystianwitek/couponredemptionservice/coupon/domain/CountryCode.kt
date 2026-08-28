package com.krystianwitek.couponredemptionservice.coupon.domain

@JvmInline
value class CountryCode private constructor(
    val value: String,
) {
    // TODO: think about better code validation
    companion object {
        fun from(value: String): CountryCode {
            val normalizedValue = value.trim().uppercase()
            require(normalizedValue.isNotEmpty()) { "Country code must not be blank" }

            return CountryCode(normalizedValue)
        }
    }
}
