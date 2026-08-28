package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toDomain
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface CouponRedemptionRepository {
    fun save(couponRedemption: CouponRedemption): CouponRedemption
}

@Repository
internal class CouponRedemptionRepositoryAdapter(
    private val jpaCouponRedemptionRepository: JpaCouponRedemptionRepository,
) : CouponRedemptionRepository {
    override fun save(couponRedemption: CouponRedemption): CouponRedemption =
        jpaCouponRedemptionRepository.save(couponRedemption.toEntity()).toDomain()
}

internal interface JpaCouponRedemptionRepository : JpaRepository<CouponRedemptionEntity, UUID>
