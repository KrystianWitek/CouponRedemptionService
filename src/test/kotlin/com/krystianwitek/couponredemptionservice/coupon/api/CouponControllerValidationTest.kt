package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(CouponController::class)
internal class CouponControllerValidationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
    ) {
        @MockitoBean
        private lateinit var couponCreationService: CouponCreationService

        @MockitoBean
        private lateinit var couponRedemptionService: CouponRedemptionService

        @ParameterizedTest
        @MethodSource("invalidCreateCouponRequests")
        fun `should return bad request for invalid coupon creation request`(request: CreateCouponRequest) {
            mockMvc
                .perform(
                    post("/coupons")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
        }

        @ParameterizedTest
        @MethodSource("invalidRedeemCouponRequests")
        fun `should return bad request for invalid coupon redemption request`(request: RedeemCouponRequest) {
            mockMvc
                .perform(
                    post("/coupons/redeem")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
        }

        private companion object {
            @JvmStatic
            fun invalidCreateCouponRequests() =
                listOf(
                    CreateCouponRequest(code = "", maxUsageCount = 10, countryCode = "PL"),
                    CreateCouponRequest(code = "A".repeat(101), maxUsageCount = 10, countryCode = "PL"),
                    CreateCouponRequest(code = "SUMMER20", maxUsageCount = 0, countryCode = "PL"),
                    CreateCouponRequest(code = "SUMMER20", maxUsageCount = 10, countryCode = ""),
                    CreateCouponRequest(code = "SUMMER20", maxUsageCount = 10, countryCode = "POL"),
                )

            @JvmStatic
            fun invalidRedeemCouponRequests() =
                listOf(
                    RedeemCouponRequest(code = "", userId = "user-123"),
                    RedeemCouponRequest(code = "A".repeat(101), userId = "user-123"),
                    RedeemCouponRequest(code = "SUMMER20", userId = ""),
                    RedeemCouponRequest(code = "SUMMER20", userId = "A".repeat(256)),
                )
        }
    }
