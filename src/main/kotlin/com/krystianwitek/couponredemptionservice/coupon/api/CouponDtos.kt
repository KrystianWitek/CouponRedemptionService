package com.krystianwitek.couponredemptionservice.coupon.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateCouponRequest(
    @NotBlank
    @Size(max = 100)
    val code: String,
    @Positive
    val maxUsageCount: Int,
    @NotBlank
    @Pattern(regexp = "[A-Za-z]{2}")
    val countryCode: String,
)

data class CouponResponse(
    val id: UUID,
    val code: String,
    val createdAt: Instant,
    val maxUsageCount: Int,
    val currentUsageCount: Int,
    val countryCode: String,
)

data class RedeemCouponRequest(
    @NotBlank
    @Size(max = 100)
    val code: String,
    @NotBlank
    @Size(max = 255)
    val userId: String,
)

data class CouponRedemptionResponse(
    val id: UUID,
    val code: String,
    val userId: String,
    val redeemedAt: Instant,
)
