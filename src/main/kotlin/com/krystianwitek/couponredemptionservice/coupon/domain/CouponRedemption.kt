package com.krystianwitek.couponredemptionservice.coupon.domain

import java.time.Instant
import java.util.UUID

data class CouponRedemption(
	val id: CouponRedemptionId,
	val couponId: CouponId,
	val userId: UserId,
	val redeemedAt: Instant,
)

@JvmInline
value class CouponRedemptionId(
    val value: UUID,
)