package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

@IntegrationTest
internal class CouponRedemptionIntegrationTest
    @Autowired
    constructor(
        private val restTestClient: RestTestClient,
        private val couponRepository: CouponRepository,
    ) {
        @MockitoBean
        private lateinit var geoIpProvider: GeoIpProvider

        @Test
        fun `should redeem coupon and persist updated usage`() {
            // given
            val coupon =
                couponRepository.save(
                    aCoupon(
                        code = CouponCode.from("REDEEM10"),
                        country = CountryCode.from("PL"),
                    ),
                )
            val request = aRedeemCouponRequest(code = coupon.code.value)
            given(geoIpProvider.resolveCountry(anyString())).willReturn(coupon.country)

            // when
            val response =
                restTestClient
                    .post()
                    .uri("/coupons/redeem")
                    .body(request)
                    .exchange()
                    .expectStatus()
                    .isCreated
                    .expectBody<CouponRedemptionResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            val updatedCoupon = couponRepository.findByCode(coupon.code)

            assertThat(response.code).isEqualTo(coupon.code.value)
            assertThat(response.userId).isEqualTo(request.userId)
            assertThat(response.redeemedAt).isNotNull
            assertThat(updatedCoupon?.currentUsageCount).isEqualTo(1)
        }
    }
