package com.krystianwitek.couponredemptionservice.coupon

import com.krystianwitek.couponredemptionservice.coupon.application.CouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.CouponRedemptionService
import com.krystianwitek.couponredemptionservice.coupon.application.DefaultCouponCreationService
import com.krystianwitek.couponredemptionservice.coupon.application.DefaultCouponRedemptionService
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionOperations

@Configuration
internal class CouponConfiguration {
    @Bean
    fun couponCreationService(couponRepository: CouponRepository): CouponCreationService = DefaultCouponCreationService(couponRepository)

    @Bean
    fun couponRedemptionService(
        couponRepository: CouponRepository,
        couponRedemptionRepository: CouponRedemptionRepository,
        geoIpProvider: GeoIpProvider,
        transactionOperations: TransactionOperations,
    ): CouponRedemptionService =
        DefaultCouponRedemptionService(
            couponRepository = couponRepository,
            couponRedemptionRepository = couponRedemptionRepository,
            geoIpProvider = geoIpProvider,
            transactionOperations = transactionOperations,
        )
}
