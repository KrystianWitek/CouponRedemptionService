package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCountryMismatchException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponNotFoundException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponUsageLimitReachedException
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.toJson
import com.krystianwitek.couponredemptionservice.toObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

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

        @ParameterizedTest
        @MethodSource("couponErrors")
        fun `should map coupon error to error response`(error: CouponError) {
            // given
            val request = aRedeemCouponRequest()
            val command = aRedeemCouponCommand(ipAddress = CLIENT_IP)
            given(couponRedemptionService.redeem(command)).willThrow(error.exception)

            // when
            val result =
                mockMvc
                    .perform(
                        post("/coupons/redeem")
                            .with { request ->
                                request.remoteAddr = CLIENT_IP
                                request
                            }.contentType(APPLICATION_JSON)
                            .content(request.toJson()),
                    ).andReturn()
            val response = result.response.contentAsString.toObject<ErrorResponse>()

            // then
            assertThat(result.response.status).isEqualTo(error.status.value())
            assertThat(response)
                .isEqualTo(
                    ErrorResponse(
                        errorCode = error.errorCode,
                        details = error.details,
                    ),
                )
        }

        data class CouponError(
            val exception: RuntimeException,
            val status: HttpStatus,
            val errorCode: ErrorResponse.ErrorCode,
            val details: String,
        )

        private companion object {
            private const val CLIENT_IP = "8.8.8.8"

            @JvmStatic
            fun couponErrors() =
                listOf(
                    CouponError(
                        exception = CouponNotFoundException(CouponCode.from("SUMMER20")),
                        status = NOT_FOUND,
                        errorCode = ErrorResponse.ErrorCode.COUPON_NOT_FOUND,
                        details = "Coupon not found: SUMMER20",
                    ),
                    CouponError(
                        exception =
                            CouponCountryMismatchException(
                                expectedCountry = CountryCode.from("PL"),
                                actualCountry = CountryCode.from("DE"),
                            ),
                        status = FORBIDDEN,
                        errorCode = ErrorResponse.ErrorCode.COUPON_COUNTRY_MISMATCH,
                        details = "Coupon is not valid for country: DE",
                    ),
                    CouponError(
                        exception = CouponUsageLimitReachedException(CouponCode.from("SUMMER20")),
                        status = CONFLICT,
                        errorCode = ErrorResponse.ErrorCode.COUPON_USAGE_LIMIT_REACHED,
                        details = "Coupon usage limit reached: SUMMER20",
                    ),
                    CouponError(
                        exception = GeoIpLookupException("GeoIP provider request failed"),
                        status = SERVICE_UNAVAILABLE,
                        errorCode = ErrorResponse.ErrorCode.GEO_IP_LOOKUP_FAILED,
                        details = "GeoIP provider request failed",
                    ),
                )
        }
    }
