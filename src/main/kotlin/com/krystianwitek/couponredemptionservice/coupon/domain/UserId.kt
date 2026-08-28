package com.krystianwitek.couponredemptionservice.coupon.domain

@JvmInline
value class UserId private constructor(
    val value: String,
) {
    companion object {
        fun from(value: String): UserId {
            val normalizedValue = value.trim()
            require(normalizedValue.isNotEmpty()) { "User ID must not be blank" }

            return UserId(normalizedValue)
        }
    }
}
