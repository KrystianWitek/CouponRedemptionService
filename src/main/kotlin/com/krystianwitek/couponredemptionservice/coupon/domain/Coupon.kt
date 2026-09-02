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
        require(currentUsageCount <= maxUsageCount) {
            "Current usage count must not exceed maximum usage count"
        }
    }

    val isExhausted: Boolean
        get() = currentUsageCount >= maxUsageCount
}

@JvmInline
value class CouponId(
    val value: UUID,
)
