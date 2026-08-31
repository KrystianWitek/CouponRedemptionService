package com.krystianwitek.couponredemptionservice.coupon.api

internal class InvalidCountryCodeException(
    val countryCode: String,
    cause: IllegalArgumentException,
) : RuntimeException("Unsupported country code: $countryCode", cause)
