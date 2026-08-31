package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
class Coupon(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),
    val code: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
    val maxUsageCount: Int,
    var currentUsageCount: Int = 0,
    val countryCode: String,
)
