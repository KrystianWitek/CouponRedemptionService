package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@IntegrationTest
@Transactional
internal class CouponRepositoryIntegrationTest @Autowired constructor(
    private val couponRepository: CouponRepository,
) {
    @Test
    fun `should save and find coupon by code`() {
        // given
        val coupon = Coupon(
            id = CouponId(UUID.randomUUID()),
            code = CouponCode.from("SUMMER20"),
            createdAt = Instant.parse("2026-08-28T10:00:00Z"),
            maxUsageCount = 10,
            currentUsageCount = 0,
            country = CountryCode.from("PL"),
        )

        // when
        couponRepository.save(coupon)
        val result = couponRepository.findByCode(CouponCode.from("summer20"))

        // then
        assertThat(result).isEqualTo(coupon)
    }
}
