package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface CouponRedemptionRepository

@Repository
internal class CouponRedemptionRepositoryAdapter(
    private val jpaCouponRedemptionRepository: JpaCouponRedemptionRepository,
) : CouponRedemptionRepository

internal interface JpaCouponRedemptionRepository : JpaRepository<CouponRedemption, UUID>
