package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence

import com.krystianwitek.couponredemptionservice.infrastructure.config.PostgresTestConfiguration
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRedemptionRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity

@DataJpaTest
@Import(PostgresTestConfiguration::class)
internal class PersistenceConstraintsIntegrationTest
    @Autowired
    constructor(
        private val couponRedemptionRepository: TestCouponRedemptionRepository,
    ) {
        @Test
        fun `should reject coupon redemption for nonexistent coupon`() {
            // expect
            assertThatThrownBy {
                couponRedemptionRepository.saveAndFlush(
                    CouponRedemptionEntity(
                        couponId = UUID.randomUUID(),
                        userId = "user-123",
                    ),
                )
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        }
    }
