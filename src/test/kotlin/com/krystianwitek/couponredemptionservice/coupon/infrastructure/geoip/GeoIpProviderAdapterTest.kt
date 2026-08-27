package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.config.GeoIpConfiguration
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Duration

class GeoIpProviderAdapterTest {
    private lateinit var wireMock: WireMockServer

    // subject
    private lateinit var provider: GeoIpProvider

    @BeforeEach
    fun setup() {
        wireMock = WireMockServer(wireMockConfig().dynamicPort())
        wireMock.start()
        provider = createProvider()
    }

    @AfterEach
    fun cleanup() {
        wireMock.stop()
    }

    @Test
    fun `should resolve country through GeoIP provider`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/8.8.8.8"))
                .withQueryParam("fields", equalTo("success,country_code,message"))
                .willReturn(okJson("""{"success":true,"country_code":"US"}""")),
        )

        // when
        val result = provider.resolveCountry("8.8.8.8")

        // then
        assertThat(result).isEqualTo(CountryCode.from("US"))
    }

    @Test
    fun `should not call provider for excluded address`() {
        // given
        val provider = createProvider(excludedAddresses = setOf("127.0.0.1"))

        // expect
        assertThatThrownBy {
            provider.resolveCountry("127.0.0.1")
        }.isInstanceOf(GeoIpLookupException::class.java)
        wireMock.verify(0, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `should reject unsuccessful provider response`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/192.0.2.1"))
                .willReturn(okJson("""{"success":false,"message":"Reserved range"}""")),
        )

        // expect
        assertThatThrownBy {
            provider.resolveCountry("192.0.2.1")
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    @ParameterizedTest
    @ValueSource(ints = [429, 500])
    fun `should wrap provider HTTP error`(status: Int) {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/8.8.4.4"))
                .willReturn(aResponse().withStatus(status)),
        )

        // expect
        assertThatThrownBy {
            provider.resolveCountry("8.8.4.4")
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    @Test
    fun `should wrap provider timeout`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/1.1.1.1"))
                .willReturn(
                    okJson("""{"success":true,"country_code":"AU"}""")
                        .withFixedDelay(500),
            ),
        )

        // expect
        assertThatThrownBy {
            createProvider(readTimeout = Duration.ofMillis(100)).resolveCountry("1.1.1.1")
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    private fun createProvider(
        readTimeout: Duration = Duration.ofSeconds(2),
        excludedAddresses: Set<String> = emptySet(),
    ): GeoIpProvider {
        val properties = GeoIpProperties(
            baseUrl = URI.create(wireMock.baseUrl()),
            connectTimeout = Duration.ofSeconds(1),
            readTimeout = readTimeout,
            excludedAddresses = excludedAddresses,
        )
        val restClient = GeoIpConfiguration().geoIpRestClient(RestClient.builder(), properties)

        return GeoIpProviderAdapter(restClient, properties)
    }
}
