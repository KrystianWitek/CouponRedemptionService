package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode

class CouponNotFoundException(
    val code: CouponCode,
) : RuntimeException("Coupon not found: ${code.value}")

class CouponCountryMismatchException(
    val expectedCountry: CountryCode,
    val actualCountry: CountryCode,
) : RuntimeException("Coupon is not valid for country: ${actualCountry.value}")

class CouponUsageLimitReachedException(
    val code: CouponCode,
) : RuntimeException("Coupon usage limit reached: ${code.value}")
