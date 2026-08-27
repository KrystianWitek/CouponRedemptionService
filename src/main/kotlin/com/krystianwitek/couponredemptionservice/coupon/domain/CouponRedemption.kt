package com.krystianwitek.couponredemptionservice.coupon.domain

import java.time.Instant

data class CouponRedemption(
	val id: CouponRedemptionId,
	val couponId: CouponId,
	val userId: UserId,
	val redeemedAt: Instant,
)
