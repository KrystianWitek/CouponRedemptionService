package com.krystianwitek.couponredemptionservice.coupon.infrastructure

import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository

internal class InMemoryCouponRepository : CouponRepository {
    private val coupons = mutableMapOf<CouponId, Coupon>()

    override fun createIfAbsent(coupon: Coupon): Boolean {
        if (findByCode(coupon.code) != null) {
            return false
        }

        coupons[coupon.id] = coupon
        return true
    }

    override fun findByCode(code: CouponCode): Coupon? = coupons.values.firstOrNull { it.code == code }

    override fun incrementUsageIfAvailable(couponId: CouponId): Boolean {
        val coupon = coupons[couponId] ?: return false

        if (coupon.currentUsageCount >= coupon.maxUsageCount) {
            return false
        }

        coupons[couponId] = coupon.copy(currentUsageCount = coupon.currentUsageCount + 1)
        return true
    }
}
