package com.krystianwitek.couponredemptionservice.coupon.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RedeemCouponRequest(
    @NotBlank
    @Size(max = 100)
    val code: String,
    @NotBlank
    @Size(max = 255)
    val userId: String,
)
