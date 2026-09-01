package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aCreateCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.aCreateCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.toJson
import com.krystianwitek.couponredemptionservice.toObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.time.Instant
import java.util.UUID

@WebMvcTest(CouponController::class)
internal class CouponControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        @MockitoBean
        private lateinit var couponCreationService: CouponCreationService

        @MockitoBean
        private lateinit var couponRedemptionService: CouponRedemptionService

        @Test
        fun `should create coupon`() {
            // given
            val request = aCreateCouponRequest(code = " summer20 ", countryCode = "pl")
            val command = aCreateCouponCommand()
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
                mockMvc
                    .perform(
                        post("/api/v1/coupons")
                            .contentType(APPLICATION_JSON)
                            .content(request.toJson()),
                    ).andReturn()
            val response = result.response.contentAsString.toObject<CouponResponse>()

            // then
            assertThat(result.response.status).isEqualTo(CREATED.value())
            assertThat(response).isEqualTo(coupon.toResponse())
            then(couponCreationService).should().create(command)
        }

        @Test
        fun `should redeem coupon`() {
            // given
            val request = aRedeemCouponRequest(code = " summer20 ", userId = " user-123 ")
            val command = aRedeemCouponCommand(ipAddress = CLIENT_IP)
            val redemption =
                CouponRedemption(
                    id = CouponRedemptionId(UUID.fromString("658f08b6-5f0f-4f9f-b4e4-7f68e53464ef")),
                    couponId = CouponId(UUID.fromString("8cf42c86-170f-4bad-b287-dcdb73323a64")),
                    userId = command.userId,
                    redeemedAt = Instant.parse("2026-08-29T12:30:00Z"),
                )
            given(couponRedemptionService.redeem(command)).willReturn(redemption)

            // when
            val result =
                mockMvc
                    .perform(
                        post("/api/v1/coupons/redeem")
                            .with { request ->
                                request.remoteAddr = CLIENT_IP
                                request
                            }.contentType(APPLICATION_JSON)
                            .content(request.toJson()),
                    ).andReturn()
            val response = result.response.contentAsString.toObject<CouponRedemptionResponse>()

            // then
            assertThat(result.response.status).isEqualTo(CREATED.value())
            assertThat(response).isEqualTo(redemption.toResponse(command.code))
            then(couponRedemptionService).should().redeem(command)
        }

        private companion object {
            const val CLIENT_IP = "8.8.8.8"
        }
    }
