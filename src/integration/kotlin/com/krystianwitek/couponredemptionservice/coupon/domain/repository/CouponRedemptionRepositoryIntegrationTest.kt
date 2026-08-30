package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aCouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

@IntegrationTest
internal class CouponRedemptionRepositoryIntegrationTest
    @Autowired
    constructor(
        private val couponRepository: CouponRepository,
        private val couponRedemptionRepository: CouponRedemptionRepository,
    ) {
        @Test
        fun `should create coupon redemption`() {
            // given
            val coupon = couponRepository.save(aCoupon())
            val couponRedemption = aCouponRedemption(couponId = coupon.id)

            // when
            val result = couponRedemptionRepository.createIfAbsent(couponRedemption)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should not create duplicate coupon redemption`() {
            // given
            val coupon = couponRepository.save(aCoupon())
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
