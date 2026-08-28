package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toDomain
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface CouponRepository {
    fun save(coupon: Coupon): Coupon

    fun findByCode(code: CouponCode): Coupon?
}

@Repository
internal class CouponRepositoryAdapter(
    private val jpaCouponRepository: JpaCouponRepository,
) : CouponRepository {
    override fun save(coupon: Coupon): Coupon = jpaCouponRepository.save(coupon.toEntity()).toDomain()

    override fun findByCode(code: CouponCode): Coupon? = jpaCouponRepository.findByCode(code.value)?.toDomain()
}

internal interface JpaCouponRepository : JpaRepository<CouponEntity, UUID> {
    fun findByCode(code: String): CouponEntity?
}
