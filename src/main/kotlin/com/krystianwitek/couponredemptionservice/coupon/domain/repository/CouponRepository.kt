package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface CouponRepository

@Repository
internal class CouponRepositoryAdapter(
    private val jpaCouponRepository: JpaCouponRepository,
) : CouponRepository

internal interface JpaCouponRepository : JpaRepository<Coupon, UUID>
