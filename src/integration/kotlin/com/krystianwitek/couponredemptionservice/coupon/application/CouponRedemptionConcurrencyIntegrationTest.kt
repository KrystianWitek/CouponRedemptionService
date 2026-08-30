package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRedemptionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

@IntegrationTest
internal class CouponRedemptionConcurrencyIntegrationTest
    @Autowired
    constructor(
        private val couponRedemptionService: CouponRedemptionService,
        private val couponRepository: CouponRepository,
        private val testCouponRedemptionRepository: TestCouponRedemptionRepository,
    ) {
        @MockitoBean
        private lateinit var geoIpProvider: GeoIpProvider

        @BeforeEach
        fun stubGeoIp() {
            given(geoIpProvider.resolveCountry(anyString())).willReturn(COUNTRY)
        }

        @Test
        fun `should redeem coupon exactly once when the same user sends concurrent requests`() {
            // given
            val coupon = couponRepository.save(aCoupon(maxUsageCount = ATTEMPTS, country = COUNTRY))
            val command = aRedeemCouponCommand(code = coupon.code)

            // when
            val outcomes = redeemConcurrently(List(ATTEMPTS) { command })

            // then
            val redemptions = outcomes.successes()
            assertThat(redemptions).hasSize(1)
            assertThat(outcomes.failures())
                .hasSize(ATTEMPTS - 1)
                .hasOnlyElementsOfType(CouponAlreadyRedeemedException::class.java)
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
            val persisted = persistedRedemptions(coupon)
            assertThat(persisted).hasSize(1)
            persisted[0].let {
                assertThat(it.id).isEqualTo(redemptions.single().id.value)
                assertThat(it.userId).isEqualTo(command.userId.value)
            }
        }

        @Test
        fun `should not exceed usage limit and should roll back losing redemptions under concurrent requests`() {
            // given
            val usageLimit = 5
            val coupon = couponRepository.save(aCoupon(maxUsageCount = usageLimit, country = COUNTRY))
            val commands = List(ATTEMPTS) { aRedeemCouponCommand(code = coupon.code, userId = UserId.from("user-$it")) }

            // when
            val outcomes = redeemConcurrently(commands)

            // then
            val redemptions = outcomes.successes()
            assertThat(redemptions).hasSize(usageLimit)
            assertThat(redemptions.map { it.userId }).doesNotHaveDuplicates()
            assertThat(outcomes.failures())
                .hasSize(ATTEMPTS - usageLimit)
                .hasOnlyElementsOfType(CouponUsageLimitReachedException::class.java)
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(usageLimit)
            assertThat(persistedRedemptions(coupon).map { it.id })
                .containsExactlyInAnyOrderElementsOf(redemptions.map { it.id.value })
        }

        private fun redeemConcurrently(commands: List<RedeemCouponCommand>): List<Result<CouponRedemption>> {
            val ready = CountDownLatch(commands.size)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(commands.size)

            try {
                val attempts =
                    commands.map { command ->
                        executor.submit<Result<CouponRedemption>> {
                            ready.countDown()
                            start.await()
                            runCatching { couponRedemptionService.redeem(command) }
                        }
                    }
                check(ready.await(10, SECONDS)) { "Workers did not reach the start gate in time" }
                start.countDown()

                return attempts.map { it.get(30, SECONDS) }
            } finally {
                executor.shutdownNow()
            }
        }

        private fun List<Result<CouponRedemption>>.successes() = mapNotNull { it.getOrNull() }

        private fun List<Result<CouponRedemption>>.failures() = mapNotNull { it.exceptionOrNull() }

        private fun persistedRedemptions(coupon: Coupon) = testCouponRedemptionRepository.findAllByCouponId(coupon.id.value)

        private companion object {
            const val ATTEMPTS = 20
            val COUNTRY = CountryCode.from("PL")
        }
    }
