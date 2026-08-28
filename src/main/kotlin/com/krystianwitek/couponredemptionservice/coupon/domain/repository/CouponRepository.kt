package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toDomain
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface CouponRepository {
    fun save(coupon: Coupon): Coupon

    fun findByCode(code: CouponCode): Coupon?

    fun incrementUsageIfAvailable(couponId: CouponId): Boolean
}

@Repository
internal class CouponRepositoryAdapter(
    private val jpaCouponRepository: JpaCouponRepository,
) : CouponRepository {
    override fun save(coupon: Coupon): Coupon = jpaCouponRepository.save(coupon.toEntity()).toDomain()

    override fun findByCode(code: CouponCode): Coupon? = jpaCouponRepository.findByCode(code.value)?.toDomain()

    @Transactional
    override fun incrementUsageIfAvailable(couponId: CouponId): Boolean =
        jpaCouponRepository.incrementUsageIfAvailable(couponId.value) == 1
}

internal interface JpaCouponRepository : JpaRepository<CouponEntity, UUID> {
    fun findByCode(code: String): CouponEntity?

    @Modifying
    @Query(
        value = """
            UPDATE coupon
            SET current_usage_count = current_usage_count + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :couponId
              AND current_usage_count < max_usage_count
        """,
        nativeQuery = true,
    )
    fun incrementUsageIfAvailable(
        @Param("couponId") couponId: UUID,
    ): Int
}
