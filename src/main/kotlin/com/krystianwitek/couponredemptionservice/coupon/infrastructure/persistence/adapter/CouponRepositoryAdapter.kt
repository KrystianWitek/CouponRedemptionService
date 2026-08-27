package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.adapter

import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.repository.JpaCouponRepository
import org.springframework.stereotype.Repository

@Repository
internal class CouponRepositoryAdapter(
    private val jpaCouponRepository: JpaCouponRepository,
) : CouponRepository
