package com.krystianwitek.couponredemptionservice.coupon.infrastructure

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository

internal class InMemoryCouponRedemptionRepository : CouponRedemptionRepository {
    private val redemptions = mutableMapOf<CouponRedemptionId, CouponRedemption>()

    override fun createIfAbsent(couponRedemption: CouponRedemption): Boolean {
        val alreadyExists =
            redemptions.values.any {
                it.couponId == couponRedemption.couponId && it.userId == couponRedemption.userId
            }
        if (alreadyExists) {
            return false
        }

        redemptions[couponRedemption.id] = couponRedemption
        return true
    }

    fun findAll(): List<CouponRedemption> = redemptions.values.toList()
}
