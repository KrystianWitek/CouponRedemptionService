package com.krystianwitek.couponredemptionservice.coupon.application

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.Coupon
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponCode
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponId
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemption
import com.krystianwitek.couponredemptionservice.coupon.domain.CouponRedemptionId
import com.krystianwitek.couponredemptionservice.coupon.domain.UserId
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRedemptionRepository
import com.krystianwitek.couponredemptionservice.coupon.domain.repository.CouponRepository
import org.springframework.transaction.support.TransactionOperations
import java.time.Instant
import java.util.UUID

interface CouponRedemptionService {
    fun redeem(command: RedeemCouponCommand): CouponRedemption
}

data class RedeemCouponCommand(
    val code: CouponCode,
    val userId: UserId,
    val ipAddress: String,
)

internal class DefaultCouponRedemptionService(
    private val couponRepository: CouponRepository,
    private val couponRedemptionRepository: CouponRedemptionRepository,
    private val geoIpProvider: GeoIpProvider,
    private val transactionOperations: TransactionOperations,
) : CouponRedemptionService {
    override fun redeem(command: RedeemCouponCommand): CouponRedemption {
        val coupon = findCoupon(command.code)
        val requestCountry = geoIpProvider.resolveCountry(command.ipAddress)

        validateCountry(coupon.country, requestCountry)

        return redeemInTransaction(coupon, command.userId)
    }

    private fun findCoupon(code: CouponCode): Coupon =
        couponRepository.findByCode(code)
            ?: throw CouponNotFoundException(code)

    private fun validateCountry(
        couponCountry: CountryCode,
        requestCountry: CountryCode,
    ) {
        if (couponCountry != requestCountry) {
            throw CouponCountryMismatchException(
                expectedCountry = couponCountry,
                actualCountry = requestCountry,
            )
        }
    }

    private fun redeemInTransaction(
        coupon: Coupon,
        userId: UserId,
    ): CouponRedemption =
        transactionOperations.execute {
            incrementUsage(coupon)
            saveRedemption(coupon.id, userId)
        } ?: error("Coupon redemption transaction returned no result")

    private fun incrementUsage(coupon: Coupon) {
        if (!couponRepository.incrementUsageIfAvailable(coupon.id)) {
            throw CouponUsageLimitReachedException(coupon.code)
        }
    }

    private fun saveRedemption(
        couponId: CouponId,
        userId: UserId,
    ): CouponRedemption =
        couponRedemptionRepository.save(
            CouponRedemption(
                id = CouponRedemptionId(UUID.randomUUID()),
                couponId = couponId,
                userId = userId,
                redeemedAt = Instant.now(),
            ),
        )
}

class CouponNotFoundException(
    val code: CouponCode,
) : RuntimeException("Coupon not found: ${code.value}")

class CouponCountryMismatchException(
    val expectedCountry: CountryCode,
    val actualCountry: CountryCode,
) : RuntimeException("Coupon is not valid for country: ${actualCountry.value}")

class CouponUsageLimitReachedException(
    val code: CouponCode,
) : RuntimeException("Coupon usage limit reached: ${code.value}")
