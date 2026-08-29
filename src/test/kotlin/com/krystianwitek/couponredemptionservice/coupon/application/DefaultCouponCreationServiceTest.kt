package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.aCreateCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.InMemoryCouponRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class DefaultCouponCreationServiceTest {
    private val couponRepository = InMemoryCouponRepository()
    private val service = DefaultCouponCreationService(couponRepository)

    @Test
    fun `should create coupon`() {
        // given
        val command = aCreateCouponCommand()
        val beforeCreation = Instant.now()

        // when
        val result = service.create(command)

        // then
        assertThat(result.code).isEqualTo(command.code)
        assertThat(result.maxUsageCount).isEqualTo(command.maxUsageCount)
        assertThat(result.currentUsageCount).isZero()
        assertThat(result.country).isEqualTo(command.countryCode)
        assertThat(result.id.value).isNotEqualTo(UUID(0, 0))
        assertThat(result.createdAt).isBetween(beforeCreation, Instant.now())
        assertThat(couponRepository.findByCode(command.code)).isEqualTo(result)
    }
}
