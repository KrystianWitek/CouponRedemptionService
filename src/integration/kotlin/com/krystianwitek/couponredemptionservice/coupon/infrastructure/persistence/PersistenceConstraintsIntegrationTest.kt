package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption
import com.krystianwitek.couponredemptionservice.infrastructure.config.PostgresTestConfiguration
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.util.UUID

@DataJpaTest
@Import(PostgresTestConfiguration::class)
internal class PersistenceConstraintsIntegrationTest @Autowired constructor(
    private val entityManager: TestEntityManager,
) {
    @Test
    fun `should reject duplicate coupon code regardless of letter case`() {
        // given
        entityManager.persistAndFlush(aCoupon(code = CouponCode.from("SUMMER20").value))

        // expect
        assertThatThrownBy {
            entityManager.persistAndFlush(aCoupon(code = CouponCode.from("summer20").value))
        }.isInstanceOf(ConstraintViolationException::class.java)
    }

    @Test
    fun `should reject duplicate coupon redemption for the same user`() {
        // given
        val coupon = entityManager.persistAndFlush(aCoupon())
        entityManager.persistAndFlush(aCouponRedemption(couponId = coupon.id))

        // expect
        assertThatThrownBy {
            entityManager.persistAndFlush(aCouponRedemption(couponId = coupon.id))
        }.isInstanceOf(ConstraintViolationException::class.java)
    }

    @Test
    fun `should reject coupon redemption for nonexistent coupon`() {
        // expect
        assertThatThrownBy {
            entityManager.persistAndFlush(aCouponRedemption(couponId = UUID.randomUUID()))
        }.isInstanceOf(ConstraintViolationException::class.java)
    }

    private fun aCoupon(code: String = "SUMMER20") =
        Coupon(
            code = code,
            maxUsageCount = 10,
            countryCode = "PL",
        )

    private fun aCouponRedemption(couponId: UUID) =
        CouponRedemption(
            couponId = couponId,
            userId = "user-123",
        )
}
