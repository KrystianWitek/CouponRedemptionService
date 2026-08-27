package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IpWhoIsResponseTest {
    @Test
    fun `should map successful response to country code`() {
        // given
        val response = IpWhoIsResponse(
            success = true,
            countryCode = "pl",
        )

        // when
        val result = response.toCountryCode()

        // then
        assertThat(result).isEqualTo(CountryCode.from("PL"))
    }

    @Test
    fun `should reject unsuccessful response`() {
        // given
        val response = IpWhoIsResponse(
            success = false,
            message = "Reserved range",
        )

        // expect
        assertThatThrownBy {
            response.toCountryCode()
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    @Test
    fun `should reject missing country code`() {
        // given
        val response = IpWhoIsResponse(
            success = true,
            countryCode = null,
        )

        // expect
        assertThatThrownBy {
            response.toCountryCode()
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "P", "POL", "12"])
    fun `should reject invalid country code`(countryCode: String) {
        // given
        val response = IpWhoIsResponse(
            success = true,
            countryCode = countryCode,
        )

        // expect
        assertThatThrownBy {
            response.toCountryCode()
        }.isInstanceOf(GeoIpLookupException::class.java)
    }
}
