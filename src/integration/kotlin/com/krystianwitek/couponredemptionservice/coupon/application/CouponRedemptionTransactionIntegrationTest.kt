package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@IntegrationTest
internal class CouponRedemptionTransactionIntegrationTest
    @Autowired
    constructor(
        private val couponRedemptionService: CouponRedemptionService,
        private val testCouponRepository: TestCouponRepository,
        private val testCouponRedemptionRepository: TestCouponRedemptionRepository,
    ) {
        @MockitoBean
        private lateinit var geoIpProvider: GeoIpProvider

        @MockitoSpyBean
        private lateinit var couponRepository: CouponRepository

        @Test
        fun `should not increment usage when coupon was already redeemed by user`() {
            // given
            val coupon = aCoupon(maxUsageCount = 2, country = COUNTRY)
            couponRepository.createIfAbsent(coupon)
            val command = aRedeemCouponCommand(code = coupon.code)
            given(geoIpProvider.resolveCountry(command.ipAddress)).willReturn(COUNTRY)
            couponRedemptionService.redeem(command)

            // when
            val exception =
                catchThrowable {
                    couponRedemptionService.redeem(command)
                }

            // then
            assertThat(exception)
                .isInstanceOf(CouponAlreadyRedeemedException::class.java)
                .hasMessage("Coupon already redeemed by user: ${command.userId.value}")
            assertThat(persistedCoupon(coupon).currentUsageCount).isEqualTo(1)
        }

        @Test
        fun `should reject exhausted coupon before opening a transaction`() {
            // given
            val coupon = aCoupon(maxUsageCount = 1, currentUsageCount = 1, country = COUNTRY)
            couponRepository.createIfAbsent(coupon)
            val command = aRedeemCouponCommand(code = coupon.code)

            // when
            val exception =
                catchThrowable {
                    couponRedemptionService.redeem(command)
                }

            // then
            assertThat(exception)
                .isInstanceOf(CouponUsageLimitReachedException::class.java)
                .hasMessage("Coupon usage limit reached: ${coupon.code.value}")
            assertThat(hasRedemption(coupon, command)).isFalse()
            assertThat(persistedCoupon(coupon).currentUsageCount).isEqualTo(1)
            verify(geoIpProvider, never()).resolveCountry(command.ipAddress)
        }

        @Test
        fun `should rollback redemption when coupon usage limit was reached`() {
            // given
            val coupon = aCoupon(maxUsageCount = 1, currentUsageCount = 1, country = COUNTRY)
            couponRepository.createIfAbsent(coupon)
            val command = aRedeemCouponCommand(code = coupon.code)
            given(geoIpProvider.resolveCountry(command.ipAddress)).willReturn(COUNTRY)
            willReturn(coupon.copy(currentUsageCount = 0)).given(couponRepository).findByCode(coupon.code)

            // when
            val exception =
                catchThrowable {
                    couponRedemptionService.redeem(command)
                }

            // then
            assertThat(exception)
                .isInstanceOf(CouponUsageLimitReachedException::class.java)
                .hasMessage("Coupon usage limit reached: ${coupon.code.value}")
            assertThat(hasRedemption(coupon, command)).isFalse()
            assertThat(persistedCoupon(coupon).currentUsageCount).isEqualTo(1)
            verify(couponRepository).incrementUsageIfAvailable(coupon.id)
        }

        private fun persistedCoupon(coupon: Coupon) = testCouponRepository.findById(coupon.id.value).get()

        private fun hasRedemption(
            coupon: Coupon,
            command: RedeemCouponCommand,
        ) = testCouponRedemptionRepository.existsByCouponIdAndUserId(
            couponId = coupon.id.value,
            userId = command.userId.value,
        )

        private companion object {
            val COUNTRY = CountryCode.from("PL")
        }
    }
