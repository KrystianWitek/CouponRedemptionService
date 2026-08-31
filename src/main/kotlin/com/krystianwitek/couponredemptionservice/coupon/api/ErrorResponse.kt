package com.krystianwitek.couponredemptionservice.coupon.api

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ErrorResponse(
    val errorCode: ErrorCode,
    val details: String,
    val invalidFields: Set<String> = emptySet(),
) {
    enum class ErrorCode {
        COUPON_NOT_FOUND,
        COUPON_ALREADY_EXISTS,
        COUPON_COUNTRY_MISMATCH,
        COUPON_USAGE_LIMIT_REACHED,
        COUPON_ALREADY_REDEEMED,
        GEO_IP_LOOKUP_FAILED,
        INVALID_COUNTRY_CODE,
        VALIDATION_ERROR,
    }
}
