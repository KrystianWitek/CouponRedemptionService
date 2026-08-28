package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class CouponMappersTest {
    @Test
    fun `should map domain coupon to entity`() {
        // given
        val coupon = Coupon(
            id = CouponId(ID),
            code = CouponCode.from(CODE),
            createdAt = CREATED_AT,
            maxUsageCount = MAX_USAGE_COUNT,
            currentUsageCount = CURRENT_USAGE_COUNT,
            country = CountryCode.from(COUNTRY_CODE),
        )

        // when
        val result = coupon.toEntity()

        // then
        assertThat(result.id).isEqualTo(ID)
        assertThat(result.code).isEqualTo(CODE)
        assertThat(result.createdAt).isEqualTo(CREATED_AT)
        assertThat(result.updatedAt).isEqualTo(CREATED_AT)
        assertThat(result.maxUsageCount).isEqualTo(MAX_USAGE_COUNT)
        assertThat(result.currentUsageCount).isEqualTo(CURRENT_USAGE_COUNT)
        assertThat(result.countryCode).isEqualTo(COUNTRY_CODE)
    }

    @Test
    fun `should map coupon entity to domain`() {
        // given
        val coupon = CouponEntity(
            id = ID,
            code = CODE,
            createdAt = CREATED_AT,
            maxUsageCount = MAX_USAGE_COUNT,
            currentUsageCount = CURRENT_USAGE_COUNT,
            countryCode = COUNTRY_CODE,
        )

        // when
        val result = coupon.toDomain()

        // then
        assertThat(result).isEqualTo(
            Coupon(
                id = CouponId(ID),
                code = CouponCode.from(CODE),
                createdAt = CREATED_AT,
                maxUsageCount = MAX_USAGE_COUNT,
                currentUsageCount = CURRENT_USAGE_COUNT,
                country = CountryCode.from(COUNTRY_CODE),
            ),
        )
    }

    private companion object {
        val ID: UUID = UUID.fromString("5dc6f1c1-4f66-4c46-b6b5-95df1060bcaa")
        val CREATED_AT: Instant = Instant.parse("2026-08-28T10:00:00Z")
        const val CODE = "SUMMER20"
        const val MAX_USAGE_COUNT = 10
        const val CURRENT_USAGE_COUNT = 3
        const val COUNTRY_CODE = "PL"
    }
}
