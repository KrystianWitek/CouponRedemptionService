package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity
import com.krystianwitek.couponredemptionservice.infrastructure.config.PostgresTestConfiguration
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@DataJpaTest
@Import(PostgresTestConfiguration::class)
internal class PersistenceConstraintsIntegrationTest @Autowired constructor(
    private val couponRepository: TestCouponRepository,
    private val couponRedemptionRepository: TestCouponRedemptionRepository,
) {
    @Test
    fun `should reject duplicate coupon code regardless of letter case`() {
        // given
        couponRepository.saveAndFlush(aCoupon(code = CouponCode.from("SUMMER20").value))

        // expect
        assertThatThrownBy {
            couponRepository.saveAndFlush(aCoupon(code = CouponCode.from("summer20").value))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `should reject duplicate coupon redemption for the same user`() {
        // given
        val coupon = couponRepository.saveAndFlush(aCoupon())
        couponRedemptionRepository.saveAndFlush(aCouponRedemption(couponId = coupon.id))

        // expect
        assertThatThrownBy {
            couponRedemptionRepository.saveAndFlush(aCouponRedemption(couponId = coupon.id))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `should reject coupon redemption for nonexistent coupon`() {
        // expect
        assertThatThrownBy {
            couponRedemptionRepository.saveAndFlush(aCouponRedemption(couponId = UUID.randomUUID()))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun aCoupon(code: String = "SUMMER20") =
        CouponEntity(
            code = code,
            maxUsageCount = 10,
            countryCode = "PL",
        )

    private fun aCouponRedemption(couponId: UUID) =
        CouponRedemptionEntity(
            couponId = couponId,
            userId = "user-123",
        )
}

internal interface TestCouponRepository : JpaRepository<CouponEntity, UUID>

internal interface TestCouponRedemptionRepository : JpaRepository<CouponRedemptionEntity, UUID>
