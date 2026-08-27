package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
class CouponRedemption(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),
    val couponId: UUID,
    val userId: String,
    val redeemedAt: Instant = Instant.now(),
)
