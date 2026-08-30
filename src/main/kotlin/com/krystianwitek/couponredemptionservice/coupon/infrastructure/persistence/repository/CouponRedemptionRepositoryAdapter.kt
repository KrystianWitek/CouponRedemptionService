package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity

@Repository
internal class CouponRedemptionRepositoryAdapter(
    private val jpaCouponRedemptionRepository: JpaCouponRedemptionRepository,
) : CouponRedemptionRepository {
    @Transactional
    override fun createIfAbsent(couponRedemption: CouponRedemption): Boolean =
        jpaCouponRedemptionRepository.insertIfAbsent(
            id = couponRedemption.id.value,
            couponId = couponRedemption.couponId.value,
            userId = couponRedemption.userId.value,
            redeemedAt = couponRedemption.redeemedAt,
        ) == 1
}

internal interface JpaCouponRedemptionRepository : JpaRepository<CouponRedemptionEntity, UUID> {
    @Modifying
    @Query(
        value = """
            INSERT INTO coupon_redemption (id, coupon_id, user_id, redeemed_at)
            VALUES (:id, :couponId, :userId, :redeemedAt)
            ON CONFLICT (coupon_id, user_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("couponId") couponId: UUID,
        @Param("userId") userId: String,
        @Param("redeemedAt") redeemedAt: Instant,
    ): Int
}
