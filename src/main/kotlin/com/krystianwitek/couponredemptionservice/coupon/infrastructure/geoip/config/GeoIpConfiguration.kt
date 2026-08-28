package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.config

import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.GeoIpProviderAdapter
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GeoIpProperties::class)
internal class GeoIpConfiguration {
    @Bean
    fun geoIpRestClient(
        restClientBuilder: RestClient.Builder,
        properties: GeoIpProperties,
    ): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.connectTimeout)
                setReadTimeout(properties.readTimeout)
            }

        return restClientBuilder
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun geoIpProvider(
        restClient: RestClient,
        properties: GeoIpProperties,
    ) = GeoIpProviderAdapter(restClient, properties)
}
