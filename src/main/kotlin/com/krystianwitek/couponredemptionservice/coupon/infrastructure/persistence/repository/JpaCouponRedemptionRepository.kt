package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.repository

import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface JpaCouponRedemptionRepository : JpaRepository<CouponRedemption, UUID>
