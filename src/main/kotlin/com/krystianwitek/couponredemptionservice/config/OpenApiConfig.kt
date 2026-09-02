package com.krystianwitek.couponredemptionservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Coupon Redemption Service")
                .description("REST API for creating country-restricted discount coupons and recording their redemption.")
                .version("v1"),
        )
}
