package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.mapper

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity.CouponRedemption as CouponRedemptionEntity

internal fun CouponRedemption.toEntity() =
    CouponRedemptionEntity(
        id = id.value,
        couponId = couponId.value,
        userId = userId.value,
        redeemedAt = redeemedAt,
    )

internal fun CouponRedemptionEntity.toDomain() =
    CouponRedemption(
        id = CouponRedemptionId(id),
        couponId = CouponId(couponId),
        userId = UserId.from(userId),
        redeemedAt = redeemedAt,
    )
