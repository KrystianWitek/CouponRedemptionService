package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aCreateCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.api.dto.CouponResponse
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import java.time.temporal.ChronoUnit.MICROS

@IntegrationTest
internal class CouponCreationIntegrationTest
    @Autowired
    constructor(
        private val restTestClient: RestTestClient,
        private val couponRepository: CouponRepository,
    ) {
        @Test
        fun `should create and persist coupon`() {
            // given
            val request = aCreateCouponRequest(code = "WELCOME10")

            // when
            val response =
                restTestClient
                    .post()
                    .uri("/api/v1/coupons")
                    .body(request)
                    .exchange()
                    .expectStatus()
                    .isCreated
                    .expectBody<CouponResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            val coupon = couponRepository.findByCode(CouponCode.from(request.code))

            assertThat(coupon).isNotNull
            with(coupon!!) {
                assertThat(response.id).isEqualTo(id.value)
                assertThat(response.code).isEqualTo(code.value)
                assertThat(response.createdAt).isCloseTo(createdAt, within(1, MICROS))
                assertThat(response.maxUsageCount).isEqualTo(maxUsageCount)
                assertThat(response.currentUsageCount).isEqualTo(currentUsageCount)
                assertThat(response.countryCode).isEqualTo(country.value)
            }
        }
    }
