package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
internal class CouponRepositoryIntegrationTest
    @Autowired
    constructor(
        private val couponRepository: CouponRepository,
    ) {
        @Test
        fun `should create and find coupon by code`() {
            // given
            val coupon =
                aCoupon(
                    code = CouponCode.from("SUMMER20"),
                    createdAt = Instant.parse("2026-01-01T12:00:00Z"),
                )

            // when
            val created = couponRepository.createIfAbsent(coupon)
            val result = couponRepository.findByCode(CouponCode.from("SUMMER20"))

            // then
            assertThat(created).isTrue()
            assertThat(result).isEqualTo(coupon)
        }

        @Test
        fun `should not replace coupon when code already exists`() {
            // given
            val existingCoupon =
                aCoupon(
                    code = CouponCode.from("WINTER20"),
                    createdAt = Instant.parse("2026-01-02T12:00:00Z"),
                )
            val duplicateCoupon = aCoupon(code = CouponCode.from("WINTER20"))
            couponRepository.createIfAbsent(existingCoupon)

            // when
            val created = couponRepository.createIfAbsent(duplicateCoupon)

            // then
            assertThat(created).isFalse()
            assertThat(couponRepository.findByCode(existingCoupon.code)).isEqualTo(existingCoupon)
        }
    }
