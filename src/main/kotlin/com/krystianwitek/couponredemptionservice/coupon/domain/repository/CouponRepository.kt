package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId

interface CouponRepository {
    fun createIfAbsent(coupon: Coupon): Boolean

    fun findByCode(code: CouponCode): Coupon?

    fun incrementUsageIfAvailable(couponId: CouponId): Boolean
}
