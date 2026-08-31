package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aCouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRedemptionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

@IntegrationTest
internal class CouponRedemptionRepositoryIntegrationTest
    @Autowired
    constructor(
        private val couponRepository: CouponRepository,
        private val couponRedemptionRepository: CouponRedemptionRepository,
        private val testCouponRedemptionRepository: TestCouponRedemptionRepository,
    ) {
        @Test
        fun `should create coupon redemption`() {
            // given
            val coupon = aCoupon()
            couponRepository.createIfAbsent(coupon)
            val couponRedemption =
                aCouponRedemption(
                    couponId = coupon.id,
                    redeemedAt = Instant.parse("2026-01-01T12:00:00Z"),
                )

            // when
            val result = couponRedemptionRepository.createIfAbsent(couponRedemption)

            // then
            assertThat(result).isTrue()
            val persistedRedemption = testCouponRedemptionRepository.findAllByCouponId(coupon.id.value).single()
            assertThat(persistedRedemption.id).isEqualTo(couponRedemption.id.value)
            assertThat(persistedRedemption.couponId).isEqualTo(couponRedemption.couponId.value)
            assertThat(persistedRedemption.userId).isEqualTo(couponRedemption.userId.value)
            assertThat(persistedRedemption.redeemedAt).isEqualTo(couponRedemption.redeemedAt)
        }

        @Test
        fun `should not create duplicate coupon redemption`() {
            // given
            val coupon = aCoupon()
            couponRepository.createIfAbsent(coupon)
            val couponRedemption = aCouponRedemption(couponId = coupon.id)
            couponRedemptionRepository.createIfAbsent(couponRedemption)

            // when
            val result =
                couponRedemptionRepository.createIfAbsent(
                    couponRedemption.copy(id = CouponRedemptionId(UUID.randomUUID())),
                )

            // then
            assertThat(result).isFalse()
        }
    }
