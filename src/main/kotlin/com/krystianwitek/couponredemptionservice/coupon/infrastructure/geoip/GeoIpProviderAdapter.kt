package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.model.IpWhoIsResponse
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

internal class GeoIpProviderAdapter(
    private val restClient: RestClient,
    private val properties: GeoIpProperties,
    private val circuitBreaker: CircuitBreaker,
) : GeoIpProvider {
    override fun resolveCountry(ipAddress: String): CountryCode {
        ensureLookupIsAllowed(ipAddress)
        return fetchResponse(ipAddress).toCountryCode()
    }

    private fun ensureLookupIsAllowed(ipAddress: String) {
        if (ipAddress in properties.excludedAddresses) {
            throw GeoIpLookupException("GeoIP resolution is disabled for this address")
        }
    }

    private fun fetchResponse(ipAddress: String): IpWhoIsResponse =
        try {
            circuitBreaker.executeSupplier { exchangeForResponse(ipAddress) }
        } catch (exception: CallNotPermittedException) {
            throw GeoIpLookupException("GeoIP provider is temporarily unavailable", exception)
        } catch (exception: RestClientException) {
            throw GeoIpLookupException("GeoIP provider request failed", exception)
        }

    private fun exchangeForResponse(ipAddress: String): IpWhoIsResponse =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .pathSegment(ipAddress)
                    .queryParam("fields", RESPONSE_FIELDS)
                    .build()
            }.retrieve()
            .body(IpWhoIsResponse::class.java)
            ?: throw GeoIpLookupException("GeoIP provider returned an empty response")

    private companion object {
        const val RESPONSE_FIELDS = "success,country_code,message"
    }
}
