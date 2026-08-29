package com.krystianwitek.couponredemptionservice.coupon

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import java.time.Instant
import java.util.UUID

fun aCoupon(
    id: CouponId = CouponId(UUID.randomUUID()),
    code: CouponCode = CouponCode.from(UUID.randomUUID().toString()),
    createdAt: Instant = Instant.now(),
    maxUsageCount: Int = 10,
    currentUsageCount: Int = 0,
    country: CountryCode = CountryCode.from("PL"),
) = Coupon(
    id = id,
    code = code,
    createdAt = createdAt,
    maxUsageCount = maxUsageCount,
    currentUsageCount = currentUsageCount,
    country = country,
)
