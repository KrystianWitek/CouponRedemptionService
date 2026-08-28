package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import java.time.Instant
import java.util.UUID

interface CouponCreationService {
    fun create(command: CreateCouponCommand): Coupon
}

data class CreateCouponCommand(
    val code: CouponCode,
    val maxUsageCount: Int,
    val countryCode: CountryCode,
)

internal class DefaultCouponCreationService(
    private val couponRepository: CouponRepository,
) : CouponCreationService {
    override fun create(command: CreateCouponCommand): Coupon = couponRepository.save(command.toCoupon())

    private fun CreateCouponCommand.toCoupon() =
        Coupon(
            id = CouponId(UUID.randomUUID()),
            code = code,
            createdAt = Instant.now(),
            maxUsageCount = maxUsageCount,
            currentUsageCount = 0,
            country = countryCode,
        )
}
