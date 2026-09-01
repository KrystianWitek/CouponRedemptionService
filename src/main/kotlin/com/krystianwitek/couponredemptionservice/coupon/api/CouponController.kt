package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Discount coupon creation and redemption")
class CouponController(
    private val couponCreationService: CouponCreationService,
    private val couponRedemptionService: CouponRedemptionService,
) {
    private val log = KotlinLogging.logger {}

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Create a coupon")
    fun createCoupon(
        @RequestBody @Valid request: CreateCouponRequest,
    ): CouponResponse {
        log.info { "[START] createCoupon [couponCode: ${request.code}]" }
        val response = couponCreationService.create(request.toCommand()).toResponse()
        log.info { "[END] createCoupon [couponId: ${response.id}]" }

        return response
    }

    @PostMapping("/redeem")
    @ResponseStatus(CREATED)
    @Operation(
        summary = "Redeem a coupon",
        description = "Resolves the caller's country from the client IP address; the coupon must match that country.",
    )
    fun redeemCoupon(
        @RequestBody @Valid request: RedeemCouponRequest,
        httpRequest: HttpServletRequest,
    ): CouponRedemptionResponse {
        log.info { "[START] redeemCoupon [couponCode: ${request.code}]" }
        val command = request.toCommand(httpRequest.remoteAddr)
        val response = couponRedemptionService.redeem(command).toResponse(command.code)
        log.info { "[END] redeemCoupon [couponRedemptionId: ${response.id}]" }

        return response
    }
}
