package com.krystianwitek.couponredemptionservice.coupon.infrastructure

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository

internal class InMemoryCouponRedemptionRepository : CouponRedemptionRepository {
    private val redemptions = mutableMapOf<CouponRedemptionId, CouponRedemption>()

    override fun save(couponRedemption: CouponRedemption): CouponRedemption {
        redemptions[couponRedemption.id] = couponRedemption
        return couponRedemption
    }

    fun findAll(): List<CouponRedemption> = redemptions.values.toList()
}
