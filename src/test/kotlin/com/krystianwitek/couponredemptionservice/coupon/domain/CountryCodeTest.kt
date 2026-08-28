package com.krystianwitek.couponredemptionservice.coupon.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CountryCodeTest {
    @Test
    fun `should normalize country code`() {
        // when
        val result = CountryCode.from(" pl ")

        // then
        assertThat(result.value).isEqualTo("PL")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "P", "POL", "P1"])
    fun `should reject invalid country code format`(value: String) {
        assertThatThrownBy {
            CountryCode.from(value)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Country code must contain exactly two letters")
    }

    @ParameterizedTest
    @ValueSource(strings = ["XX", "ZZ", "EU", "XK"])
    fun `should reject unsupported country code`(value: String) {
        assertThatThrownBy {
            CountryCode.from(value)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Unsupported country code: $value")
    }
}
