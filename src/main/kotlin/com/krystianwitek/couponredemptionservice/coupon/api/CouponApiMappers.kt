package com.krystianwitek.couponredemptionservice.coupon.api

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
        countryCode = CountryCode.from(countryCode),
    )

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
