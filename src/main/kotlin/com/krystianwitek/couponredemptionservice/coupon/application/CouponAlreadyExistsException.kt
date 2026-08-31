package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode

class CouponAlreadyExistsException(
    val code: CouponCode,
) : RuntimeException("Coupon already exists: ${code.value}")
