package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository

interface CreateCouponUseCase {
    fun create(command: CreateCouponCommand): Coupon
}

data class CreateCouponCommand(
    val code: CouponCode,
    val maxUsageCount: Int,
    val countryCode: CountryCode,
)

internal class CreateCouponService(
    private val couponRepository: CouponRepository,
) : CreateCouponUseCase {
    override fun create(command: CreateCouponCommand): Coupon = TODO("Needs implementation")
}
