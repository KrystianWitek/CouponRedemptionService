package com.krystianwitek.couponredemptionservice.coupon.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coupons")
class CouponController {
    @PostMapping
    fun createCoupon(
        @RequestBody @Valid request: CreateCouponRequest,
    ): ResponseEntity<CouponResponse> = TODO("Needs implementation")

    @PostMapping("/redeem")
    fun redeemCoupon(
        @RequestBody @Valid request: RedeemCouponRequest,
    ): ResponseEntity<CouponRedemptionResponse> = TODO("Needs implementation")
}
