package com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip

import com.krystianwitek.couponredemptionservice.coupon.domain.CountryCode
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpLookupException
import com.krystianwitek.couponredemptionservice.coupon.domain.geoip.GeoIpProvider
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.model.IpWhoIsResponse
import com.krystianwitek.couponredemptionservice.coupon.infrastructure.geoip.properties.GeoIpProperties
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

internal class GeoIpProviderAdapter(
    private val restClient: RestClient,
    private val properties: GeoIpProperties,
) : GeoIpProvider {
    override fun resolveCountry(ipAddress: String): CountryCode {
        if (ipAddress in properties.excludedAddresses) {
            throw GeoIpLookupException("GeoIP resolution is disabled for this address")
        }

        return try {
            val response =
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

            response.toCountryCode()
        } catch (exception: GeoIpLookupException) {
            throw exception
        } catch (exception: RestClientException) {
            throw GeoIpLookupException("GeoIP provider request failed", exception)
        }
    }

    private companion object {
        const val RESPONSE_FIELDS = "success,country_code,message"
    }
}
