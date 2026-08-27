package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository

interface CouponRedemptionService {
    fun redeem(command: RedeemCouponCommand): CouponRedemption
}

data class RedeemCouponCommand(
    val code: CouponCode,
    val userId: UserId,
    val ipAddress: String,
)

internal class DefaultCouponRedemptionService(
    private val couponRepository: CouponRepository,
    private val couponRedemptionRepository: CouponRedemptionRepository,
    private val geoIpProvider: GeoIpProvider,
) : CouponRedemptionService {
    override fun redeem(command: RedeemCouponCommand): CouponRedemption = TODO("Needs implementation")
}
