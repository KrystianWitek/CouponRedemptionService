package com.krystianwitek.couponredemptionservice.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity

internal interface TestCouponRepository : JpaRepository<CouponEntity, UUID>

internal interface TestCouponRedemptionRepository : JpaRepository<CouponRedemptionEntity, UUID> {
    fun existsByCouponIdAndUserId(
        couponId: UUID,
        userId: String,
    ): Boolean

    fun findAllByCouponId(couponId: UUID): List<CouponRedemptionEntity>
}
