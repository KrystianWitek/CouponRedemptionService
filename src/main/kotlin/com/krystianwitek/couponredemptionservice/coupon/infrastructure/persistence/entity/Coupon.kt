package com.krystianwitek.couponredemptionservice.coupon.infrastructure.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import java.time.Instant
import java.util.UUID

@Entity
class Coupon(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),
    val code: String,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = createdAt,
    val maxUsageCount: Int,
    var currentUsageCount: Int = 0,
    val countryCode: String,
) {
    @PreUpdate
    fun setUpdatedAt() {
        updatedAt = Instant.now()
    }
}
