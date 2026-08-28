package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.model

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class IpWhoIsResponseTest {
    @Test
    fun `should map successful response to country code`() {
        // given
        val response =
            IpWhoIsResponse(
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
        val response =
            IpWhoIsResponse(
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
        val response =
            IpWhoIsResponse(
                success = true,
                countryCode = null,
            )

        // expect
        assertThatThrownBy {
            response.toCountryCode()
        }.isInstanceOf(GeoIpLookupException::class.java)
    }

    @Test
    fun `should reject invalid country code`() {
        // given
        val response =
            IpWhoIsResponse(
                success = true,
                countryCode = "XX",
            )

        // expect
        assertThatThrownBy {
            response.toCountryCode()
        }.isInstanceOf(GeoIpLookupException::class.java)
            .hasMessage("GeoIP provider returned an invalid country code")
            .hasCauseInstanceOf(IllegalArgumentException::class.java)
    }
}
