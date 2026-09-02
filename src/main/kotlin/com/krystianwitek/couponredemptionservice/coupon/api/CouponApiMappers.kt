package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.api.dto.CouponRedemptionResponse
import com.krystianwitek.couponredemptionservice.coupon.api.dto.CouponResponse
import com.krystianwitek.couponredemptionservice.coupon.api.dto.CreateCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.api.dto.RedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.application.CreateCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.application.RedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId

internal fun CreateCouponRequest.toCommand() =
    CreateCouponCommand(
        code = CouponCode.from(code),
        maxUsageCount = maxUsageCount,
        countryCode = countryCode.toCountryCode(),
    )

private fun String.toCountryCode(): CountryCode =
    try {
        CountryCode.from(this)
    } catch (exception: IllegalArgumentException) {
        throw InvalidCountryCodeException(this, exception)
    }

internal fun Coupon.toResponse() =
    CouponResponse(
        id = id.value,
        code = code.value,
        createdAt = createdAt,
        maxUsageCount = maxUsageCount,
        currentUsageCount = currentUsageCount,
        countryCode = country.value,
    )

internal fun RedeemCouponRequest.toCommand(ipAddress: String) =
    RedeemCouponCommand(
        code = CouponCode.from(code),
        userId = UserId.from(userId),
        ipAddress = ipAddress,
    )

internal fun CouponRedemption.toResponse(code: CouponCode) =
    CouponRedemptionResponse(
        id = id.value,
        code = code.value,
        userId = userId.value,
        redeemedAt = redeemedAt,
    )
