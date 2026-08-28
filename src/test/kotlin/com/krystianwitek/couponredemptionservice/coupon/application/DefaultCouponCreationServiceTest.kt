package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class DefaultCouponCreationServiceTest {
    private val couponRepository = CouponRepositoryFake()
    private val service = DefaultCouponCreationService(couponRepository)

    @Test
    fun `should create coupon`() {
        // given
        val command =
            CreateCouponCommand(
                code = CouponCode.from("SUMMER20"),
                maxUsageCount = 10,
                countryCode = CountryCode.from("PL"),
            )
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
        assertThat(couponRepository.savedCoupon).isSameAs(result)
    }

    private class CouponRepositoryFake : CouponRepository {
        lateinit var savedCoupon: Coupon

        override fun save(coupon: Coupon): Coupon = coupon.also { savedCoupon = it }

        override fun findByCode(code: CouponCode): Coupon? = error("Not expected in this test")

        override fun incrementUsageIfAvailable(couponId: CouponId): Boolean = error("Not expected in this test")
    }
}
