package com.krystianwitek.couponredemptionservice.coupon.api.dto
import java.time.Instant
import java.util.UUID

data class CouponRedemptionResponse(
    val id: UUID,
    val code: String,
    val userId: String,
    val redeemedAt: Instant,
)
