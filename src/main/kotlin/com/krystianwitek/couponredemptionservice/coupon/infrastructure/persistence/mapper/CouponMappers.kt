package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.Coupon as CouponEntity

internal fun Coupon.toEntity() =
    CouponEntity(
        id = id.value,
        code = code.value,
        createdAt = createdAt,
        maxUsageCount = maxUsageCount,
        currentUsageCount = currentUsageCount,
        countryCode = country.value,
    )

internal fun CouponEntity.toDomain() =
    Coupon(
        id = CouponId(id),
        code = CouponCode.from(code),
        createdAt = createdAt,
        maxUsageCount = maxUsageCount,
        currentUsageCount = currentUsageCount,
        country = CountryCode.from(countryCode),
    )
