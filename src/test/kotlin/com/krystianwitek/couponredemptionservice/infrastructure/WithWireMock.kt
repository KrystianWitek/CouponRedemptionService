package com.krystianwitek.couponredemptionservice.infrastructure

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

interface WithWireMock {
    val wireMock: WireMockServer
        get() = wireMockServer

    @BeforeEach
    fun startWireMock() {
        wireMock.start()
        wireMock.resetAll()
    }

    @AfterEach
    fun stopWireMock() {
        wireMock.stop()
    }

    private companion object {
        val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    }
}
