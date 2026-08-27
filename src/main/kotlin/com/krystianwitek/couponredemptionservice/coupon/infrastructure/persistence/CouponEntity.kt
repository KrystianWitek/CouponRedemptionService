package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "coupon")
class CouponEntity(
	@field:Id
	@field:Column(name = "id", nullable = false, updatable = false)
	val id: UUID,
	@field:Column(name = "code", nullable = false, length = 100)
	val code: String,
	@field:Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: Instant,
	@field:Column(name = "updated_at", nullable = false)
	var updatedAt: Instant,
	@field:Column(name = "max_usage_count", nullable = false)
	val maxUsageCount: Int,
	@field:Column(name = "current_usage_count", nullable = false)
	var currentUsageCount: Int,
	@field:Column(name = "country_code", nullable = false, length = 2)
	val countryCode: String,
)
