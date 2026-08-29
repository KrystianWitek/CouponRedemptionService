package com.krystianwitek.couponredemptionservice.coupon.api

import com.krystianwitek.couponredemptionservice.coupon.application.CreateCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode

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
