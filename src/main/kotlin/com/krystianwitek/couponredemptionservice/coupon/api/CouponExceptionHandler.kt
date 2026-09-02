package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_ALREADY_EXISTS
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_ALREADY_REDEEMED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_COUNTRY_MISMATCH
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_NOT_FOUND
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.COUPON_USAGE_LIMIT_REACHED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.GEO_IP_LOOKUP_FAILED
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.INVALID_COUNTRY_CODE
import com.krystianwitek.couponredemptionservice.coupon.api.ErrorResponse.ErrorCode.VALIDATION_ERROR
import com.krystianwitek.couponredemptionservice.coupon.application.CouponAlreadyExistsException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponAlreadyRedeemedException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponCountryMismatchException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponNotFoundException
import com.krystianwitek.couponredemptionservice.coupon.application.CouponUsageLimitReachedException
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [CouponController::class])
internal class CouponExceptionHandler {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(CouponAlreadyExistsException::class)
    @ResponseStatus(CONFLICT)
    fun handleCouponAlreadyExists(exception: CouponAlreadyExistsException): ErrorResponse {
        log.debug { "Coupon request rejected. [errorCode: $COUPON_ALREADY_EXISTS, couponCode: ${exception.code.value}]" }
        return exception.toErrorResponse(COUPON_ALREADY_EXISTS)
    }

    @ExceptionHandler(InvalidCountryCodeException::class)
    @ResponseStatus(BAD_REQUEST)
    fun handleInvalidCountryCode(exception: InvalidCountryCodeException): ErrorResponse {
        log.debug { "Coupon request rejected. [errorCode: $INVALID_COUNTRY_CODE, countryCode: ${exception.countryCode}]" }
        return exception.toErrorResponse(INVALID_COUNTRY_CODE)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(BAD_REQUEST)
    fun handleValidation(exception: MethodArgumentNotValidException): ErrorResponse {
        val invalidFields =
            exception.bindingResult.fieldErrors
                .map { it.field }
                .toSortedSet()
        log.debug { "Coupon request rejected. [errorCode: $VALIDATION_ERROR, invalidFields: $invalidFields]" }

        return ErrorResponse(
            errorCode = VALIDATION_ERROR,
            details = VALIDATION_FAILURE_DETAILS,
            invalidFields = invalidFields,
        )
    }

    @ExceptionHandler(CouponNotFoundException::class)
    @ResponseStatus(NOT_FOUND)
    fun handleCouponNotFound(exception: CouponNotFoundException): ErrorResponse {
        log.debug { "Coupon request rejected. [errorCode: $COUPON_NOT_FOUND, couponCode: ${exception.code.value}]" }
        return exception.toErrorResponse(COUPON_NOT_FOUND)
    }

    @ExceptionHandler(CouponCountryMismatchException::class)
    @ResponseStatus(FORBIDDEN)
    fun handleCouponCountryMismatch(exception: CouponCountryMismatchException): ErrorResponse {
        log.debug {
            "Coupon request rejected. [errorCode: $COUPON_COUNTRY_MISMATCH, " +
                "expectedCountry: ${exception.expectedCountry.value}, actualCountry: ${exception.actualCountry.value}]"
        }
        return exception.toErrorResponse(COUPON_COUNTRY_MISMATCH)
    }

    @ExceptionHandler(CouponUsageLimitReachedException::class)
    @ResponseStatus(CONFLICT)
    fun handleCouponUsageLimitReached(exception: CouponUsageLimitReachedException): ErrorResponse {
        log.debug { "Coupon request rejected. [errorCode: $COUPON_USAGE_LIMIT_REACHED, couponCode: ${exception.code.value}]" }
        return exception.toErrorResponse(COUPON_USAGE_LIMIT_REACHED)
    }

    @ExceptionHandler(CouponAlreadyRedeemedException::class)
    @ResponseStatus(CONFLICT)
    fun handleCouponAlreadyRedeemed(exception: CouponAlreadyRedeemedException): ErrorResponse {
        log.debug { "Coupon request rejected. [errorCode: $COUPON_ALREADY_REDEEMED, couponId: ${exception.couponId.value}]" }
        return exception.toErrorResponse(COUPON_ALREADY_REDEEMED)
    }

    @ExceptionHandler(GeoIpLookupException::class)
    @ResponseStatus(SERVICE_UNAVAILABLE)
    fun handleGeoIpLookupFailure(exception: GeoIpLookupException): ErrorResponse {
        val cause = NestedExceptionUtils.getMostSpecificCause(exception)
        log.warn { "GeoIP lookup failed. [causeType: ${cause.javaClass.simpleName}, cause: ${cause.message}]" }

        return ErrorResponse(
            errorCode = GEO_IP_LOOKUP_FAILED,
            details = GEO_IP_LOOKUP_FAILURE_DETAILS,
        )
    }

    private companion object {
        const val GEO_IP_LOOKUP_FAILURE_DETAILS = "Unable to resolve request country"
        const val VALIDATION_FAILURE_DETAILS = "Request validation failed"
    }
}

private fun Throwable.toErrorResponse(errorCode: ErrorResponse.ErrorCode): ErrorResponse =
    ErrorResponse(
        errorCode = errorCode,
        details = message ?: errorCode.name,
    )
