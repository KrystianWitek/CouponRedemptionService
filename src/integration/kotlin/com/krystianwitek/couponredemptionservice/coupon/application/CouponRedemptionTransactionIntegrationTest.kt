package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRedemptionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

@IntegrationTest
internal class CouponRedemptionTransactionIntegrationTest
    @Autowired
    constructor(
        private val couponRedemptionService: CouponRedemptionService,
        private val couponRepository: CouponRepository,
        private val testCouponRedemptionRepository: TestCouponRedemptionRepository,
    ) {
        @MockitoBean
        private lateinit var geoIpProvider: GeoIpProvider

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
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
        }

        @Test
        fun `should rollback redemption when coupon usage limit was reached`() {
            // given
            val coupon =
                aCoupon(
                    maxUsageCount = 1,
                    currentUsageCount = 1,
                    country = COUNTRY,
                )
            couponRepository.createIfAbsent(coupon)
            val command = aRedeemCouponCommand(code = coupon.code)
            given(geoIpProvider.resolveCountry(command.ipAddress)).willReturn(COUNTRY)

            // when
            val exception =
                catchThrowable {
                    couponRedemptionService.redeem(command)
                }

            // then
            assertThat(exception)
                .isInstanceOf(CouponUsageLimitReachedException::class.java)
                .hasMessage("Coupon usage limit reached: ${coupon.code.value}")
            assertThat(
                testCouponRedemptionRepository.existsByCouponIdAndUserId(
                    couponId = coupon.id.value,
                    userId = command.userId.value,
                ),
            ).isFalse()
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
        }

        private companion object {
            val COUNTRY = CountryCode.from("PL")
        }
    }
