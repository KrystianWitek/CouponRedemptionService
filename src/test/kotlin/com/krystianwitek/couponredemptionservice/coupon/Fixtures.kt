package com.krystianwitek.couponredemptionservice.coupon

import com.krystianwitek.couponredemptionservice.coupon.api.CreateCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.api.RedeemCouponRequest
import com.krystianwitek.couponredemptionservice.coupon.application.CreateCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.application.RedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import java.time.Instant
import java.util.UUID

fun aCreateCouponRequest(
    code: String = "SUMMER20",
    maxUsageCount: Int = 10,
    countryCode: String = "PL",
) = CreateCouponRequest(
    code = code,
    maxUsageCount = maxUsageCount,
    countryCode = countryCode,
)

fun aCreateCouponCommand(
    code: CouponCode = CouponCode.from("SUMMER20"),
    maxUsageCount: Int = 10,
    countryCode: CountryCode = CountryCode.from("PL"),
) = CreateCouponCommand(
    code = code,
    maxUsageCount = maxUsageCount,
    countryCode = countryCode,
)

fun aRedeemCouponRequest(
    code: String = "SUMMER20",
    userId: String = "user-123",
) = RedeemCouponRequest(
    code = code,
    userId = userId,
)

fun aRedeemCouponCommand(
    code: CouponCode = CouponCode.from("SUMMER20"),
    userId: UserId = UserId.from("user-123"),
    ipAddress: String = "8.8.8.8",
) = RedeemCouponCommand(
    code = code,
    userId = userId,
    ipAddress = ipAddress,
)

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

fun aCouponRedemption(
    couponId: CouponId,
    id: CouponRedemptionId = CouponRedemptionId(UUID.randomUUID()),
    userId: UserId = UserId.from("user-123"),
    redeemedAt: Instant = Instant.now(),
) = CouponRedemption(
    id = id,
    couponId = couponId,
    userId = userId,
    redeemedAt = redeemedAt,
)
