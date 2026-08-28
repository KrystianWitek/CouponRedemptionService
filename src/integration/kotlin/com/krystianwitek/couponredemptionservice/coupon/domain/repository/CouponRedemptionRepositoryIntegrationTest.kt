package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

@IntegrationTest
internal class CouponRedemptionRepositoryIntegrationTest @Autowired constructor(
    private val couponRepository: CouponRepository,
    private val couponRedemptionRepository: CouponRedemptionRepository,
) {
    @Test
    fun `should save coupon redemption`() {
        // given
        val couponId = CouponId(UUID.randomUUID())
        couponRepository.save(
            Coupon(
                id = couponId,
                code = CouponCode.from("REDEMPTION20"),
                createdAt = Instant.parse("2026-08-28T10:00:00Z"),
                maxUsageCount = 10,
                currentUsageCount = 0,
                country = CountryCode.from("PL"),
            ),
        )
        val couponRedemption = CouponRedemption(
            id = CouponRedemptionId(UUID.randomUUID()),
            couponId = couponId,
            userId = UserId.from("user-123"),
            redeemedAt = Instant.parse("2026-08-28T10:05:00Z"),
        )

        // when
        val result = couponRedemptionRepository.save(couponRedemption)

        // then
        assertThat(result).isEqualTo(couponRedemption)
    }
}
