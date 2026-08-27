package com.krystianwitek.couponredemptionservice.coupon

import com.krystianwitek.couponredemptionservice.coupon.application.CreateCouponService
import com.krystianwitek.couponredemptionservice.coupon.application.CreateCouponUseCase
import com.krystianwitek.couponredemptionservice.coupon.application.RedeemCouponService
import com.krystianwitek.couponredemptionservice.coupon.application.RedeemCouponUseCase
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.GeoIpProviderAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
internal class CouponConfiguration {
    @Bean
    fun createCouponUseCase(
        couponRepository: CouponRepository,
    ): CreateCouponUseCase = CreateCouponService(couponRepository)

    @Bean
    fun redeemCouponUseCase(
        couponRepository: CouponRepository,
        couponRedemptionRepository: CouponRedemptionRepository,
        geoIpProvider: GeoIpProvider,
    ): RedeemCouponUseCase = RedeemCouponService(
        couponRepository = couponRepository,
        couponRedemptionRepository = couponRedemptionRepository,
        geoIpProvider = geoIpProvider,
    )

    @Bean
    fun geoIpProvider(
        restClientBuilder: RestClient.Builder,
    ): GeoIpProvider = GeoIpProviderAdapter(restClientBuilder)
}
