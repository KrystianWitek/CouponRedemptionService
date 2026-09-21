package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.config.GeoIpConfiguration
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties.CircuitBreakerProperties
import com.krystianwitek.couponredemptionservice.infrastructure.WithWireMock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Duration

class GeoIpProviderAdapterTest : WithWireMock {
    // subject
    private lateinit var adapter: GeoIpProviderAdapter

    @BeforeEach
    fun setup() {
        adapter = createAdapter()
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
        val result = adapter.resolveCountry("8.8.8.8")

        // then
        assertThat(result).isEqualTo(CountryCode.from("US"))
    }

    @Test
    fun `should not call provider for excluded address`() {
        // given
        val adapter = createAdapter(excludedAddresses = setOf("127.0.0.1"))

        // expect
        assertThatThrownBy {
            adapter.resolveCountry("127.0.0.1")
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
            adapter.resolveCountry("192.0.2.1")
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
            adapter.resolveCountry("8.8.4.4")
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
            createAdapter(readTimeout = Duration.ofMillis(100)).resolveCountry("1.1.1.1")
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    @Test
    fun `should fail fast once repeated provider errors open the circuit breaker`() {
        // given
        val adapter = createAdapter(circuitBreakerProperties = smallCircuitBreakerProperties())
        wireMock.stubFor(
            get(urlPathEqualTo("/8.8.4.4"))
                .willReturn(aResponse().withStatus(500)),
        )

        // when
        repeat(4) {
            assertThatThrownBy {
                adapter.resolveCountry("8.8.4.4")
            }.isInstanceOf(GeoIpLookupException::class.java)
        }

        // then
        assertThatThrownBy {
            adapter.resolveCountry("8.8.4.4")
        }.isInstanceOf(GeoIpLookupException::class.java)
            .hasMessage("GeoIP provider is temporarily unavailable")
        wireMock.verify(4, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `should not count excluded address lookups towards the circuit breaker`() {
        // given
        val adapter =
            createAdapter(
                excludedAddresses = setOf("127.0.0.1"),
                circuitBreakerProperties = smallCircuitBreakerProperties(minimumNumberOfCalls = 2),
            )
        wireMock.stubFor(
            get(urlPathEqualTo("/8.8.8.8"))
                .willReturn(okJson("""{"success":true,"country_code":"US"}""")),
        )

        // when
        repeat(5) {
            assertThatThrownBy {
                adapter.resolveCountry("127.0.0.1")
            }.isInstanceOf(GeoIpLookupException::class.java)
        }

        // then
        assertThat(adapter.resolveCountry("8.8.8.8")).isEqualTo(CountryCode.from("US"))
        wireMock.verify(1, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `should count empty provider responses towards opening the circuit breaker`() {
        // given
        val adapter = createAdapter(circuitBreakerProperties = smallCircuitBreakerProperties())
        wireMock.stubFor(
            get(urlPathEqualTo("/8.8.8.8"))
                .willReturn(aResponse().withStatus(200)),
        )

        // when
        repeat(4) {
            assertThatThrownBy {
                adapter.resolveCountry("8.8.8.8")
            }.isInstanceOf(GeoIpLookupException::class.java)
        }

        // then
        assertThatThrownBy {
            adapter.resolveCountry("8.8.8.8")
        }.isInstanceOf(GeoIpLookupException::class.java)
        wireMock.verify(4, anyRequestedFor(anyUrl()))
    }

    private fun createAdapter(
        readTimeout: Duration = Duration.ofSeconds(2),
        excludedAddresses: Set<String> = emptySet(),
        circuitBreakerProperties: CircuitBreakerProperties = CircuitBreakerProperties(),
    ): GeoIpProviderAdapter {
        val properties =
            GeoIpProperties(
                baseUrl = URI.create(wireMock.baseUrl()),
                connectTimeout = Duration.ofSeconds(1),
                readTimeout = readTimeout,
                excludedAddresses = excludedAddresses,
                circuitBreaker = circuitBreakerProperties,
            )
        val configuration = GeoIpConfiguration()
        val restClient = configuration.geoIpRestClient(RestClient.builder(), properties)
        val circuitBreaker = configuration.geoIpCircuitBreaker(configuration.geoIpCircuitBreakerRegistry(properties))

        return GeoIpProviderAdapter(restClient, properties, circuitBreaker)
    }

    private fun smallCircuitBreakerProperties(minimumNumberOfCalls: Int = 4): CircuitBreakerProperties =
        CircuitBreakerProperties(
            slidingWindowSize = 4,
            minimumNumberOfCalls = minimumNumberOfCalls,
            waitDurationInOpenState = Duration.ofMinutes(1),
        )
}
