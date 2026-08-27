package com.krystianwitek.couponredemptionservice.coupon.domain

import java.time.Instant
import java.util.UUID

data class Coupon(
	val id: CouponId,
	val code: CouponCode,
	val createdAt: Instant,
	val maxUsageCount: Int,
	val currentUsageCount: Int,
	val country: CountryCode,
) {
	init {
		require(maxUsageCount > 0) { "Maximum usage count must be greater than zero" }
		require(currentUsageCount >= 0) { "Current usage count must not be negative" }
		require(currentUsageCount <= maxUsageCount) {
			"Current usage count must not exceed maximum usage count"
		}
	}
}

@JvmInline
value class CouponId(
    val value: UUID,
)