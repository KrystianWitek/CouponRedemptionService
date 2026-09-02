package com.krystianwitek.couponredemptionservice.coupon.api.dto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

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
