package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.VALIDATION_ERROR
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import com.krystianwitek.couponredemptionservice.toJson
import com.krystianwitek.couponredemptionservice.toObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(CouponController::class)
internal class CouponControllerValidationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        @MockitoBean
        private lateinit var couponCreationService: CouponCreationService

        @MockitoBean
        private lateinit var couponRedemptionService: CouponRedemptionService

        @ParameterizedTest
        @MethodSource("invalidCreateCouponRequests")
        fun `should return bad request for invalid coupon creation request`(
            request: CreateCouponRequest,
            invalidField: String,
        ) {
            // when
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/coupons")
                            .contentType(APPLICATION_JSON)
                            .content(request.toJson()),
                    ).andReturn()
                    .response

            // then
            assertValidationError(response, invalidField)
        }

        @ParameterizedTest
        @MethodSource("invalidRedeemCouponRequests")
        fun `should return bad request for invalid coupon redemption request`(
            request: RedeemCouponRequest,
            invalidField: String,
        ) {
            // when
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/coupons/redeem")
                            .contentType(APPLICATION_JSON)
                            .content(request.toJson()),
                    ).andReturn()
                    .response

            // then
            assertValidationError(response, invalidField)
        }

        private fun assertValidationError(
            response: MockHttpServletResponse,
            invalidField: String,
        ) {
            assertThat(response.status).isEqualTo(BAD_REQUEST.value())
            assertThat(response.contentAsString.toObject<ErrorResponse>())
                .isEqualTo(
                    ErrorResponse(
                        errorCode = VALIDATION_ERROR,
                        details = "Request validation failed",
                        invalidFields = setOf(invalidField),
                    ),
                )
        }

        private companion object {
            @JvmStatic
            fun invalidCreateCouponRequests() =
                listOf(
                    arrayOf(CreateCouponRequest(code = "", maxUsageCount = 10, countryCode = "PL"), "code"),
                    arrayOf(CreateCouponRequest(code = "A".repeat(101), maxUsageCount = 10, countryCode = "PL"), "code"),
                    arrayOf(CreateCouponRequest(code = "SUMMER20", maxUsageCount = 0, countryCode = "PL"), "maxUsageCount"),
                    arrayOf(CreateCouponRequest(code = "SUMMER20", maxUsageCount = 10, countryCode = ""), "countryCode"),
                    arrayOf(CreateCouponRequest(code = "SUMMER20", maxUsageCount = 10, countryCode = "POL"), "countryCode"),
                )

            @JvmStatic
            fun invalidRedeemCouponRequests() =
                listOf(
                    arrayOf(RedeemCouponRequest(code = "", userId = "user-123"), "code"),
                    arrayOf(RedeemCouponRequest(code = "A".repeat(101), userId = "user-123"), "code"),
                    arrayOf(RedeemCouponRequest(code = "SUMMER20", userId = ""), "userId"),
                    arrayOf(RedeemCouponRequest(code = "SUMMER20", userId = "A".repeat(256)), "userId"),
                )
        }
    }
