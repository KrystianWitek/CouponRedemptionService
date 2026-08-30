package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_ALREADY_REDEEMED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_COUNTRY_MISMATCH
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_NOT_FOUND
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_USAGE_LIMIT_REACHED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.GEO_IP_LOOKUP_FAILED
import com.krystianwitek.couponredemptionservice.coupon.application.CouponAlreadyRedeemedException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCountryMismatchException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponNotFoundException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponUsageLimitReachedException
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [CouponController::class])
internal class CouponExceptionHandler {
    @ExceptionHandler(CouponNotFoundException::class)
    @ResponseStatus(NOT_FOUND)
    fun handleCouponNotFound(exception: CouponNotFoundException): ErrorResponse = exception.toErrorResponse(COUPON_NOT_FOUND)

    @ExceptionHandler(CouponCountryMismatchException::class)
    @ResponseStatus(FORBIDDEN)
    fun handleCouponCountryMismatch(exception: CouponCountryMismatchException): ErrorResponse =
        exception.toErrorResponse(COUPON_COUNTRY_MISMATCH)

    @ExceptionHandler(CouponUsageLimitReachedException::class)
    @ResponseStatus(CONFLICT)
    fun handleCouponUsageLimitReached(exception: CouponUsageLimitReachedException): ErrorResponse =
        exception.toErrorResponse(COUPON_USAGE_LIMIT_REACHED)

    @ExceptionHandler(CouponAlreadyRedeemedException::class)
    @ResponseStatus(CONFLICT)
    fun handleCouponAlreadyRedeemed(exception: CouponAlreadyRedeemedException): ErrorResponse =
        exception.toErrorResponse(COUPON_ALREADY_REDEEMED)

    @ExceptionHandler(GeoIpLookupException::class)
    @ResponseStatus(SERVICE_UNAVAILABLE)
    fun handleGeoIpLookupFailure(exception: GeoIpLookupException): ErrorResponse = exception.toErrorResponse(GEO_IP_LOOKUP_FAILED)
}

private fun Throwable.toErrorResponse(errorCode: ErrorResponse.ErrorCode): ErrorResponse =
    ErrorResponse(
        errorCode = errorCode,
        details = message ?: errorCode.name,
    )
