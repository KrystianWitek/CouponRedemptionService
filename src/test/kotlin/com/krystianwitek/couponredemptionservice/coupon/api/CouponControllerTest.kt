package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import com.krystianwitek.couponredemptionservice.coupon.application.CreateCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@WebMvcTest(CouponController::class)
internal class CouponControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
    ) {
        @MockitoBean
        private lateinit var couponCreationService: CouponCreationService

        @MockitoBean
        private lateinit var couponRedemptionService: CouponRedemptionService

        @Test
        fun `should create coupon`() {
            // given
            val request = CreateCouponRequest(code = " summer20 ", maxUsageCount = 10, countryCode = "pl")
            val command =
                CreateCouponCommand(
                    code = CouponCode.from("SUMMER20"),
                    maxUsageCount = 10,
                    countryCode = CountryCode.from("PL"),
                )
            val coupon =
                aCoupon(
                    id = CouponId(UUID.fromString("73486697-7974-4d90-8689-037c7c99c876")),
                    code = command.code,
                    createdAt = Instant.parse("2026-08-29T10:15:30Z"),
                    maxUsageCount = command.maxUsageCount,
                    country = command.countryCode,
                )
            given(couponCreationService.create(command)).willReturn(coupon)

            // when
            val result =
                mockMvc.perform(
                    post("/coupons")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )

            // then
            result
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(coupon.id.value.toString()))
                .andExpect(jsonPath("$.code").value("SUMMER20"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-29T10:15:30Z"))
                .andExpect(jsonPath("$.maxUsageCount").value(10))
                .andExpect(jsonPath("$.currentUsageCount").value(0))
                .andExpect(jsonPath("$.countryCode").value("PL"))
            then(couponCreationService).should().create(command)
        }
    }
