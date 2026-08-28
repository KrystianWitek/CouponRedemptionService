package com.krystianwitek.couponredemptionservice.infrastructure

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension

internal object WireMockTestConfiguration {
    fun createExtension(): WireMockExtension =
        WireMockExtension
            .newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()
}
