package com.krystianwitek.couponredemptionservice.coupon.api

data class ErrorResponse(
    val errorCode: ErrorCode,
    val details: String,
) {
    enum class ErrorCode {
        COUPON_NOT_FOUND,
        COUPON_ALREADY_EXISTS,
        COUPON_COUNTRY_MISMATCH,
        COUPON_USAGE_LIMIT_REACHED,
        COUPON_ALREADY_REDEEMED,
        GEO_IP_LOOKUP_FAILED,
    }
}
