package com.krystianwitek.couponredemptionservice.coupon.domain.repository

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.infrastructure.config.PostgresTestConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest(
    includeFilters = [
        ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = [Repository::class],
        ),
    ],
)
@Import(PostgresTestConfiguration::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
internal class CouponUsageIntegrationTest @Autowired constructor(
    private val couponRepository: CouponRepository,
) {
    @Test
    fun `should increment coupon usage when limit is not reached`() {
        // given
        val coupon = couponRepository.save(aCoupon(maxUsageCount = 2))

        // when
        val incremented = couponRepository.incrementUsageIfAvailable(coupon.id)

        // then
        assertThat(incremented).isTrue()
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
    }

    @Test
    fun `should not increment coupon usage when limit is reached`() {
        // given
        val coupon = couponRepository.save(aCoupon(maxUsageCount = 1, currentUsageCount = 1))

        // when
        val incremented = couponRepository.incrementUsageIfAvailable(coupon.id)

        // then
        assertThat(incremented).isFalse()
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
    }

    @Test
    fun `should not exceed coupon usage limit during concurrent updates`() {
        // given
        val usageLimit = 5
        val attempts = 20
        val coupon = couponRepository.save(aCoupon(maxUsageCount = usageLimit))
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(attempts)

        try {
            val updates = (1..attempts).map {
                executor.submit<Boolean> {
                    start.await()
                    couponRepository.incrementUsageIfAvailable(coupon.id)
                }
            }

            // when
            start.countDown()
            val successfulUpdates = updates.count { it.get(10, TimeUnit.SECONDS) }

            // then
            assertThat(successfulUpdates).isEqualTo(usageLimit)
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(usageLimit)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun aCoupon(
        maxUsageCount: Int,
        currentUsageCount: Int = 0,
    ) = Coupon(
        id = CouponId(UUID.randomUUID()),
        code = CouponCode.from(UUID.randomUUID().toString()),
        createdAt = Instant.now(),
        maxUsageCount = maxUsageCount,
        currentUsageCount = currentUsageCount,
        country = CountryCode.from("PL"),
    )
}
