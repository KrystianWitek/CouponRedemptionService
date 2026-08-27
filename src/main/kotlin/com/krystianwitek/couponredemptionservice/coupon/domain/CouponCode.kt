package com.krystianwitek.couponredemptionservice.coupon.domain

import java.util.Locale

@JvmInline
value class CouponCode private constructor(
	val value: String,
) {
	companion object {
		fun from(value: String): CouponCode {
			val normalizedValue = value.trim().uppercase(Locale.ROOT)
			require(normalizedValue.isNotEmpty()) { "Coupon code must not be blank" }

			return CouponCode(normalizedValue)
		}
	}
}
