package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "coupon_redemption")
class CouponRedemptionEntity(
	@field:Id
	@field:Column(name = "id", nullable = false, updatable = false)
	val id: UUID,
	@field:Column(name = "coupon_id", nullable = false, updatable = false)
	val couponId: UUID,
	@field:Column(name = "user_id", nullable = false, updatable = false, length = 255)
	val userId: String,
	@field:Column(name = "redeemed_at", nullable = false, updatable = false)
	val redeemedAt: Instant,
)
