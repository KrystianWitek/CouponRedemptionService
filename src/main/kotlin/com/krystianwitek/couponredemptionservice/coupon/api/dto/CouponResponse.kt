package com.krystianwitek.couponredemptionservice.coupon.api.dto
import java.time.Instant
import java.util.UUID

data class CouponResponse(
    val id: UUID,
    val code: String,
    val createdAt: Instant,
    val maxUsageCount: Int,
    val currentUsageCount: Int,
    val countryCode: String,
)
