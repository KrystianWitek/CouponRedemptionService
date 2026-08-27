package com.krystianwitek.couponredemptionservice.coupon.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
class CouponController {
    @PostMapping("/coupons")
    fun createCoupon(
        @RequestBody request: CreateCouponRequest,
    ): ResponseEntity<CouponResponse> = TODO("Needs implementation")

    @PostMapping("/coupon-redemptions")
    fun redeemCoupon(
        @RequestBody request: RedeemCouponRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<CouponRedemptionResponse> = TODO("Needs implementation")
}

data class CreateCouponRequest(
    val code: String,
    val maxUsageCount: Int,
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
    val code: String,
    val userId: String,
)

data class CouponRedemptionResponse(
    val id: UUID,
    val code: String,
    val userId: String,
    val redeemedAt: Instant,
)
