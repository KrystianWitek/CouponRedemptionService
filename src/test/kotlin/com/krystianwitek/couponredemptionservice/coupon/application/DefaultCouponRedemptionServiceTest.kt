package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.aCoupon
import com.krystianwitek.couponredemptionservice.coupon.aCouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.aRedeemCouponCommand
import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.FakeTransactionOperations
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.InMemoryCouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.InMemoryCouponRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class DefaultCouponRedemptionServiceTest {
    private val couponRepository = InMemoryCouponRepository()
    private val couponRedemptionRepository = InMemoryCouponRedemptionRepository()
    private val geoIpProvider = FixedCountryGeoIpProvider(REQUEST_COUNTRY)
    private val service =
        DefaultCouponRedemptionService(
            couponRepository = couponRepository,
            couponRedemptionRepository = couponRedemptionRepository,
            geoIpProvider = geoIpProvider,
            transactionOperations = FakeTransactionOperations(),
        )

    @Test
    fun `should redeem coupon`() {
        // given
        val command = aRedeemCouponCommand()
        val coupon = aCoupon(code = command.code, country = REQUEST_COUNTRY)
        couponRepository.createIfAbsent(coupon)
        val beforeRedemption = Instant.now()

        // when
        val result = service.redeem(command)

        // then
        assertThat(result.id.value).isNotEqualTo(UUID(0, 0))
        assertThat(result.couponId).isEqualTo(coupon.id)
        assertThat(result.userId).isEqualTo(command.userId)
        assertThat(result.redeemedAt).isBetween(beforeRedemption, Instant.now())
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
        assertThat(couponRedemptionRepository.findAll()).containsExactly(result)
    }

    @Test
    fun `should reject redemption when coupon does not exist`() {
        // given
        val command = aRedeemCouponCommand()

        // when
        val exception =
            catchThrowable {
                service.redeem(command)
            }

        // then
        assertThat(exception)
            .isInstanceOf(CouponNotFoundException::class.java)
            .hasMessage("Coupon not found: ${command.code.value}")
        assertThat(couponRedemptionRepository.findAll()).isEmpty()
    }

    @Test
    fun `should reject redemption when country does not match`() {
        // given
        val command = aRedeemCouponCommand()
        val coupon = aCoupon(code = command.code, country = CountryCode.from("DE"))
        couponRepository.createIfAbsent(coupon)

        // when
        val exception =
            catchThrowable {
                service.redeem(command)
            }

        // then
        assertThat(exception)
            .isInstanceOf(CouponCountryMismatchException::class.java)
            .hasMessage("Coupon is not valid for country: ${REQUEST_COUNTRY.value}")
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isZero()
        assertThat(couponRedemptionRepository.findAll()).isEmpty()
    }

    @Test
    fun `should reject redemption when country lookup fails`() {
        // given
        val command = aRedeemCouponCommand()
        val coupon = aCoupon(code = command.code, country = REQUEST_COUNTRY)
        couponRepository.createIfAbsent(coupon)
        val serviceWithFailingGeoIp =
            DefaultCouponRedemptionService(
                couponRepository = couponRepository,
                couponRedemptionRepository = couponRedemptionRepository,
                geoIpProvider =
                    object : GeoIpProvider {
                        override fun resolveCountry(ipAddress: String): CountryCode =
                            throw GeoIpLookupException("GeoIP provider request failed")
                    },
                transactionOperations = FakeTransactionOperations(),
            )

        // when
        val exception =
            catchThrowable {
                serviceWithFailingGeoIp.redeem(command)
            }

        // then
        assertThat(exception)
            .isInstanceOf(GeoIpLookupException::class.java)
            .hasMessage("GeoIP provider request failed")
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isZero()
        assertThat(couponRedemptionRepository.findAll()).isEmpty()
    }

    @Test
    fun `should reject redemption when user already redeemed coupon`() {
        // given
        val command = aRedeemCouponCommand()
        val coupon = aCoupon(code = command.code, country = REQUEST_COUNTRY)
        couponRepository.createIfAbsent(coupon)
        val existingRedemption = aCouponRedemption(couponId = coupon.id, userId = command.userId)
        couponRedemptionRepository.createIfAbsent(existingRedemption)

        // when
        val exception =
            catchThrowable {
                service.redeem(command)
            }

        // then
        assertThat(exception)
            .isInstanceOf(CouponAlreadyRedeemedException::class.java)
            .hasMessage("Coupon already redeemed by user: ${command.userId.value}")
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isZero()
        assertThat(couponRedemptionRepository.findAll()).containsExactly(existingRedemption)
    }

    @Test
    fun `should reject redemption when usage limit is reached`() {
        // given
        val command = aRedeemCouponCommand()
        val coupon =
            aCoupon(
                code = command.code,
                maxUsageCount = 1,
                currentUsageCount = 1,
                country = REQUEST_COUNTRY,
            )
        couponRepository.createIfAbsent(coupon)

        // when
        val exception =
            catchThrowable {
                service.redeem(command)
            }

        // then
        assertThat(exception)
            .isInstanceOf(CouponUsageLimitReachedException::class.java)
            .hasMessage("Coupon usage limit reached: ${coupon.code.value}")
        assertThat(couponRepository.findByCode(coupon.code)?.currentUsageCount).isEqualTo(1)
        assertThat(couponRedemptionRepository.findAll()).isEmpty()
    }

    private companion object {
        val REQUEST_COUNTRY = CountryCode.from("PL")
    }
}

private class FixedCountryGeoIpProvider(
    private val country: CountryCode,
) : GeoIpProvider {
    override fun resolveCountry(ipAddress: String): CountryCode = country
}
