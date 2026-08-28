package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption

interface CouponRedemptionRepository {
    fun save(couponRedemption: CouponRedemption): CouponRedemption
}
