package com.krystianwitek.couponredemptionservice.coupon.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CouponCodeTest {
    @ParameterizedTest
    @ValueSource(strings = [" wiosna ", "Wiosna"])
    fun `should normalize coupon code`(value: String) {
        // when
        val result = CouponCode.from(value)

        // then
        assertThat(result.value).isEqualTo("WIOSNA")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `should reject blank coupon code`(value: String) {
        assertThatThrownBy {
            CouponCode.from(value)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Coupon code must not be blank")
    }
}
