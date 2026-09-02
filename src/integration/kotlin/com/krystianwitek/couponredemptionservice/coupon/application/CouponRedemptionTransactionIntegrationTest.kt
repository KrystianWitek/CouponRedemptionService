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
import org.mockito.BDDMockito.willAnswer
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
        fun `should reject exhausted coupon before the country lookup`() {
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
            val persistedCoupon = testCouponRepository.findById(coupon.id.value).get()
            assertThat(exception)
                .isInstanceOf(CouponUsageLimitReachedException::class.java)
                .hasMessage("Coupon usage limit reached: ${coupon.code.value}")
            assertThat(hasRedemption(coupon, command)).isFalse()
            assertThat(persistedCoupon.currentUsageCount).isEqualTo(1)
            verify(geoIpProvider, never()).resolveCountry(command.ipAddress)
        }

        @Test
        fun `should roll back the inserted redemption when usage increment loses the race`() {
            // given
            val coupon = aCoupon(maxUsageCount = 1, currentUsageCount = 0, country = COUNTRY)
            couponRepository.createIfAbsent(coupon)
            val command = aRedeemCouponCommand(code = coupon.code)
            given(geoIpProvider.resolveCountry(command.ipAddress)).willReturn(COUNTRY)
            val redemptionVisibleInTransaction = mutableListOf<Boolean>()
            willAnswer {
                redemptionVisibleInTransaction.add(hasRedemption(coupon, command))
                false
            }.given(couponRepository).incrementUsageIfAvailable(coupon.id)

            // when
            val exception =
                catchThrowable {
                    couponRedemptionService.redeem(command)
                }

            // then
            val persistedCoupon = testCouponRepository.findById(coupon.id.value).get()
            assertThat(exception)
                .isInstanceOf(CouponUsageLimitReachedException::class.java)
                .hasMessage("Coupon usage limit reached: ${coupon.code.value}")
            assertThat(redemptionVisibleInTransaction).containsExactly(true)
            assertThat(hasRedemption(coupon, command)).isFalse()
            assertThat(persistedCoupon.currentUsageCount).isZero()
        }

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
