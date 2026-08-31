package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toDomain
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity

@Repository
internal class CouponRepositoryAdapter(
    private val jpaCouponRepository: JpaCouponRepository,
) : CouponRepository {
    @Transactional
    override fun createIfAbsent(coupon: Coupon): Boolean =
        jpaCouponRepository.insertIfAbsent(
            id = coupon.id.value,
            code = coupon.code.value,
            createdAt = coupon.createdAt,
            updatedAt = coupon.createdAt,
            maxUsageCount = coupon.maxUsageCount,
            currentUsageCount = coupon.currentUsageCount,
            countryCode = coupon.country.value,
        ) == 1

    override fun findByCode(code: CouponCode): Coupon? = jpaCouponRepository.findByCode(code.value)?.toDomain()

    @Transactional
    override fun incrementUsageIfAvailable(couponId: CouponId): Boolean = jpaCouponRepository.incrementUsageIfAvailable(couponId.value) == 1
}

internal interface JpaCouponRepository : JpaRepository<CouponEntity, UUID> {
    fun findByCode(code: String): CouponEntity?

    @Modifying
    @Query(
        value = """
            INSERT INTO coupon (id, code, created_at, updated_at, max_usage_count, current_usage_count, country_code)
            VALUES (:id, :code, :createdAt, :updatedAt, :maxUsageCount, :currentUsageCount, :countryCode)
            ON CONFLICT (code) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("code") code: String,
        @Param("createdAt") createdAt: Instant,
        @Param("updatedAt") updatedAt: Instant,
        @Param("maxUsageCount") maxUsageCount: Int,
        @Param("currentUsageCount") currentUsageCount: Int,
        @Param("countryCode") countryCode: String,
    ): Int

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
