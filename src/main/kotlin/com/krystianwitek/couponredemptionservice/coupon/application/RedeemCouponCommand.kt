package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId

data class RedeemCouponCommand(
    val code: CouponCode,
    val userId: UserId,
    val ipAddress: String,
)