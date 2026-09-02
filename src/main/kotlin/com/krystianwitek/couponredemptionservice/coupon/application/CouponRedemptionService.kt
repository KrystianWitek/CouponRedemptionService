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
import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val log = KotlinLogging.logger {}

    override fun redeem(command: RedeemCouponCommand): CouponRedemption {
        log.debug { "Redeeming coupon started. [couponCode: ${command.code.value}]" }
        val coupon = findCoupon(command.code)
        rejectExhaustedCoupon(coupon)

        val requestCountry = geoIpProvider.resolveCountry(command.ipAddress)
        validateCountry(coupon.country, requestCountry)

        val couponRedemption = redeemInTransaction(coupon, command.userId)
        log.debug { "Redeeming coupon finished. [couponRedemptionId: ${couponRedemption.id.value}]" }

        return couponRedemption
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

    private fun rejectExhaustedCoupon(coupon: Coupon) {
        if (coupon.isExhausted) {
            throw CouponUsageLimitReachedException(coupon.code)
        }
    }

    private fun redeemInTransaction(
        coupon: Coupon,
        userId: UserId,
    ): CouponRedemption =
        transactionOperations.execute {
            val couponRedemption = createRedemption(coupon.id, userId)

            saveRedemption(couponRedemption)
            incrementUsage(coupon)

            couponRedemption
        }

    private fun incrementUsage(coupon: Coupon) {
        if (!couponRepository.incrementUsageIfAvailable(coupon.id)) {
            throw CouponUsageLimitReachedException(coupon.code)
        }
    }

    private fun createRedemption(
        couponId: CouponId,
        userId: UserId,
    ): CouponRedemption =
        CouponRedemption(
            id = CouponRedemptionId(UUID.randomUUID()),
            couponId = couponId,
            userId = userId,
            redeemedAt = Instant.now(),
        )

    private fun saveRedemption(couponRedemption: CouponRedemption) {
        if (!couponRedemptionRepository.createIfAbsent(couponRedemption)) {
            throw CouponAlreadyRedeemedException(
                couponId = couponRedemption.couponId,
                userId = couponRedemption.userId,
            )
        }
    }
}
