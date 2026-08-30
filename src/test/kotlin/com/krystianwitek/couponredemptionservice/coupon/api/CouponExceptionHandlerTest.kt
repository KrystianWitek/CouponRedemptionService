package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_ALREADY_REDEEMED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_COUNTRY_MISMATCH
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_NOT_FOUND
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_USAGE_LIMIT_REACHED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.GEO_IP_LOOKUP_FAILED
import com.krystianwitek.couponredemptionservice.coupon.application.CouponAlreadyRedeemedException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCountryMismatchException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponNotFoundException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponUsageLimitReachedException
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.toJson
import com.krystianwitek.couponredemptionservice.toObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.UUID

@WebMvcTest(CouponController::class)
internal class CouponExceptionHandlerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        @MockitoBean
        private lateinit var couponCreationService: CouponCreationService

        @MockitoBean
        private lateinit var couponRedemptionService: CouponRedemptionService

        @Test
        fun `should return not found when coupon does not exist`() {
            // given
            given(couponRedemptionService.redeem(REDEEM_COUPON_COMMAND))
                .willThrow(CouponNotFoundException(CouponCode.from("SUMMER20")))

            // when
            val response = redeemCoupon()

            // then
            assertErrorResponse(
                response = response,
                status = NOT_FOUND,
                errorCode = COUPON_NOT_FOUND,
                details = "Coupon not found: SUMMER20",
            )
        }

        @Test
        fun `should return forbidden when coupon country does not match`() {
            // given
            given(couponRedemptionService.redeem(REDEEM_COUPON_COMMAND))
                .willThrow(
                    CouponCountryMismatchException(
                        expectedCountry = CountryCode.from("PL"),
                        actualCountry = CountryCode.from("DE"),
                    ),
                )

            // when
            val response = redeemCoupon()

            // then
            assertErrorResponse(
                response = response,
                status = FORBIDDEN,
                errorCode = COUPON_COUNTRY_MISMATCH,
                details = "Coupon is not valid for country: DE",
            )
        }

        @Test
        fun `should return conflict when coupon usage limit is reached`() {
            // given
            given(couponRedemptionService.redeem(REDEEM_COUPON_COMMAND))
                .willThrow(CouponUsageLimitReachedException(CouponCode.from("SUMMER20")))

            // when
            val response = redeemCoupon()

            // then
            assertErrorResponse(
                response = response,
                status = CONFLICT,
                errorCode = COUPON_USAGE_LIMIT_REACHED,
                details = "Coupon usage limit reached: SUMMER20",
            )
        }

        @Test
        fun `should return conflict when coupon was already redeemed by user`() {
            // given
            given(couponRedemptionService.redeem(REDEEM_COUPON_COMMAND))
                .willThrow(
                    CouponAlreadyRedeemedException(
                        couponId = CouponId(UUID.randomUUID()),
                        userId = REDEEM_COUPON_COMMAND.userId,
                    ),
                )

            // when
            val response = redeemCoupon()

            // then
            assertErrorResponse(
                response = response,
                status = CONFLICT,
                errorCode = COUPON_ALREADY_REDEEMED,
                details = "Coupon already redeemed by user: ${REDEEM_COUPON_COMMAND.userId.value}",
            )
        }

        @Test
        fun `should return service unavailable when GeoIP lookup fails`() {
            // given
            given(couponRedemptionService.redeem(REDEEM_COUPON_COMMAND))
                .willThrow(GeoIpLookupException("GeoIP provider request failed"))

            // when
            val response = redeemCoupon()

            // then
            assertErrorResponse(
                response = response,
                status = SERVICE_UNAVAILABLE,
                errorCode = GEO_IP_LOOKUP_FAILED,
                details = "GeoIP provider request failed",
            )
        }

        private fun redeemCoupon(): MockHttpServletResponse =
            mockMvc
                .perform(
                    post("/coupons/redeem")
                        .with { request ->
                            request.remoteAddr = CLIENT_IP
                            request
                        }.contentType(APPLICATION_JSON)
                        .content(aRedeemCouponRequest().toJson()),
                ).andReturn()
                .response

        private fun assertErrorResponse(
            response: MockHttpServletResponse,
            status: HttpStatus,
            errorCode: ErrorCode,
            details: String,
        ) {
            assertThat(response.status).isEqualTo(status.value())
            assertThat(response.contentAsString.toObject<ErrorResponse>())
                .isEqualTo(
                    ErrorResponse(
                        errorCode = errorCode,
                        details = details,
                    ),
                )
        }

        private companion object {
            private const val CLIENT_IP = "8.8.8.8"
            private val REDEEM_COUPON_COMMAND = aRedeemCouponCommand(ipAddress = CLIENT_IP)
        }
    }
