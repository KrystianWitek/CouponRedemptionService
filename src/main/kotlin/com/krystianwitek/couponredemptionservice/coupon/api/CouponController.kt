package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coupons")
class CouponController(
    private val couponCreationService: CouponCreationService,
    private val couponRedemptionService: CouponRedemptionService,
) {
    @PostMapping
    fun createCoupon(
        @RequestBody @Valid request: CreateCouponRequest,
    ): ResponseEntity<CouponResponse> = TODO("Needs implementation")

    @PostMapping("/redeem")
    fun redeemCoupon(
        @RequestBody @Valid request: RedeemCouponRequest,
    ): ResponseEntity<CouponRedemptionResponse> = TODO("Needs implementation")
}
