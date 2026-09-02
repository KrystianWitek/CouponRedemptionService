package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode

data class CreateCouponCommand(
    val code: CouponCode,
    val maxUsageCount: Int,
    val countryCode: CountryCode,
)