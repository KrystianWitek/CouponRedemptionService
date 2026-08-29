package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class CouponRepositoryIntegrationTest
    @Autowired
    constructor(
        private val couponRepository: CouponRepository,
    ) {
        @Test
        fun `should save and find coupon by code`() {
            // given
            val coupon = aCoupon(code = CouponCode.from("SUMMER20"))

            // when
            couponRepository.save(coupon)
            val result = couponRepository.findByCode(CouponCode.from("summer20"))

            // then
            assertThat(result).isEqualTo(coupon)
        }
    }
