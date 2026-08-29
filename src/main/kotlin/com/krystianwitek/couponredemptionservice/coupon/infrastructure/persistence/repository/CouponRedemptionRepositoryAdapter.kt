package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.repository

import com.krystianwitek.couponredemptionservice.coupon.application.CouponAlreadyRedeemedException
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toDomain
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper.toEntity
import org.hibernate.exception.ConstraintViolationException
import org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity

@Repository
internal class CouponRedemptionRepositoryAdapter(
    private val jpaCouponRedemptionRepository: JpaCouponRedemptionRepository,
) : CouponRedemptionRepository {
    override fun save(couponRedemption: CouponRedemption): CouponRedemption =
        try {
            jpaCouponRedemptionRepository.saveAndFlush(couponRedemption.toEntity()).toDomain()
        } catch (exception: DataIntegrityViolationException) {
            if (exception.isDuplicateRedemption()) {
                throw CouponAlreadyRedeemedException(
                    couponId = couponRedemption.couponId,
                    userId = couponRedemption.userId,
                    cause = exception,
                )
            }

            throw exception
        }
}

internal interface JpaCouponRedemptionRepository : JpaRepository<CouponRedemptionEntity, UUID>

private fun DataIntegrityViolationException.isDuplicateRedemption(): Boolean =
    generateSequence<Throwable>(this) { it.cause }
        .filterIsInstance<ConstraintViolationException>()
        .any {
            it.kind == UNIQUE &&
                it.constraintName == COUPON_REDEMPTION_UNIQUE_CONSTRAINT
        }

private const val COUPON_REDEMPTION_UNIQUE_CONSTRAINT = "coupon_redemption_coupon_id_user_id_key"
