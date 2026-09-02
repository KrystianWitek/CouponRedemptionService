package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_ALREADY_REDEEMED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_COUNTRY_MISMATCH
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.GEO_IP_LOOKUP_FAILED
import com.krystianwitek.couponredemptionservice.coupon.api.dto.CouponRedemptionResponse
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.infrastructure.IntegrationTest
import com.krystianwitek.couponredemptionservice.infrastructure.persistence.TestCouponRedemptionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import java.time.temporal.ChronoUnit.MICROS

@IntegrationTest
internal class CouponRedemptionIntegrationTest
    @Autowired
    constructor(
        private val restTestClient: RestTestClient,
        private val couponRepository: CouponRepository,
        private val testCouponRedemptionRepository: TestCouponRedemptionRepository,
    ) {
        @MockitoBean
        private lateinit var geoIpProvider: GeoIpProvider

        @Test
        fun `should redeem coupon and persist updated usage`() {
            // given
            val coupon =
                aCoupon(
                    code = CouponCode.from("REDEEM10"),
                    country = CountryCode.from("PL"),
                )
            couponRepository.createIfAbsent(coupon)
            val request = aRedeemCouponRequest(code = coupon.code.value)
            given(geoIpProvider.resolveCountry(anyString())).willReturn(coupon.country)

            // when
            val response =
                restTestClient
                    .post()
                    .uri("/api/v1/coupons/redeem")
                    .body(request)
                    .exchange()
                    .expectStatus()
                    .isCreated
                    .expectBody<CouponRedemptionResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            val updatedCoupon = couponRepository.findByCode(coupon.code)
            val persistedRedemption = testCouponRedemptionRepository.findAllByCouponId(coupon.id.value).single()

            assertThat(response.id).isEqualTo(persistedRedemption.id)
            assertThat(response.code).isEqualTo(coupon.code.value)
            assertThat(response.userId).isEqualTo(request.userId)
            assertThat(response.redeemedAt).isCloseTo(persistedRedemption.redeemedAt, within(1, MICROS))
            assertThat(updatedCoupon?.currentUsageCount).isEqualTo(1)
        }

        @Test
        fun `should reject redemption from a different country`() {
            // given
            val coupon =
                aCoupon(
                    code = CouponCode.from("COUNTRY10"),
                    country = CountryCode.from("PL"),
                )
            couponRepository.createIfAbsent(coupon)
            val request = aRedeemCouponRequest(code = coupon.code.value)
            given(geoIpProvider.resolveCountry(anyString())).willReturn(CountryCode.from("DE"))

            // when
            val response =
                restTestClient
                    .post()
                    .uri("/api/v1/coupons/redeem")
                    .body(request)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(FORBIDDEN)
                    .expectBody<ErrorResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            assertThat(response.errorCode).isEqualTo(COUPON_COUNTRY_MISMATCH)
            assertThat(response.details).isEqualTo("Coupon is not valid for country: DE")
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isZero()
            assertThat(testCouponRedemptionRepository.findAllByCouponId(coupon.id.value)).isEmpty()
        }

        @Test
        fun `should reject second redemption by the same user`() {
            // given
            val coupon =
                aCoupon(
                    code = CouponCode.from("REPEAT10"),
                    country = CountryCode.from("PL"),
                )
            couponRepository.createIfAbsent(coupon)
            val request = aRedeemCouponRequest(code = coupon.code.value)
            given(geoIpProvider.resolveCountry(anyString())).willReturn(coupon.country)
            restTestClient
                .post()
                .uri("/api/v1/coupons/redeem")
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated

            // when
            val response =
                restTestClient
                    .post()
                    .uri("/api/v1/coupons/redeem")
                    .body(request)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(CONFLICT)
                    .expectBody<ErrorResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            assertThat(response.errorCode).isEqualTo(COUPON_ALREADY_REDEEMED)
            assertThat(response.details).isEqualTo("Coupon already redeemed by user: ${request.userId}")
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
            assertThat(testCouponRedemptionRepository.findAllByCouponId(coupon.id.value)).hasSize(1)
        }

        @Test
        fun `should reject redemption when country lookup fails`() {
            // given
            val coupon =
                aCoupon(
                    code = CouponCode.from("GEOIP10"),
                    country = CountryCode.from("PL"),
                )
            couponRepository.createIfAbsent(coupon)
            val request = aRedeemCouponRequest(code = coupon.code.value)
            given(geoIpProvider.resolveCountry(anyString()))
                .willThrow(GeoIpLookupException("GeoIP provider request failed"))

            // when
            val response =
                restTestClient
                    .post()
                    .uri("/api/v1/coupons/redeem")
                    .body(request)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(SERVICE_UNAVAILABLE)
                    .expectBody<ErrorResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            assertThat(response.errorCode).isEqualTo(GEO_IP_LOOKUP_FAILED)
            assertThat(response.details).isEqualTo("Unable to resolve request country")
            assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isZero()
            assertThat(testCouponRedemptionRepository.findAllByCouponId(coupon.id.value)).isEmpty()
        }
    }
