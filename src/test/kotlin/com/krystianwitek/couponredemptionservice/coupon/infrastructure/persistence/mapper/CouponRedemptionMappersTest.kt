package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class CouponRedemptionMappersTest {
    @Test
    fun `should map domain coupon redemption to entity`() {
        // given
        val couponRedemption = CouponRedemption(
            id = CouponRedemptionId(ID),
            couponId = CouponId(COUPON_ID),
            userId = UserId.from(USER_ID),
            redeemedAt = REDEEMED_AT,
        )

        // when
        val result = couponRedemption.toEntity()

        // then
        assertThat(result.id).isEqualTo(ID)
        assertThat(result.couponId).isEqualTo(COUPON_ID)
        assertThat(result.userId).isEqualTo(USER_ID)
        assertThat(result.redeemedAt).isEqualTo(REDEEMED_AT)
    }

    @Test
    fun `should map coupon redemption entity to domain`() {
        // given
        val couponRedemption = CouponRedemptionEntity(
            id = ID,
            couponId = COUPON_ID,
            userId = USER_ID,
            redeemedAt = REDEEMED_AT,
        )

        // when
        val result = couponRedemption.toDomain()

        // then
        assertThat(result).isEqualTo(
            CouponRedemption(
                id = CouponRedemptionId(ID),
                couponId = CouponId(COUPON_ID),
                userId = UserId.from(USER_ID),
                redeemedAt = REDEEMED_AT,
            ),
        )
    }

    private companion object {
        val ID: UUID = UUID.fromString("0386c407-b5f6-4a58-9725-c08fb675d949")
        val COUPON_ID: UUID = UUID.fromString("68500a50-65b3-4eb2-9cc7-0b5eabbc25de")
        val REDEEMED_AT: Instant = Instant.parse("2026-08-28T10:00:00Z")
        const val USER_ID = "user-123"
    }
}
