package com.krystianwitek.couponredemptionservice.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

@IntegrationTest
internal class ReadinessHealthIntegrationTest
    @Autowired
    constructor(
        private val restTestClient: RestTestClient,
    ) {
        @Test
        fun `should include database in readiness health`() {
            // when
            val response =
                restTestClient
                    .get()
                    .uri("/actuator/health/readiness")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody<HealthResponse>()
                    .returnResult()
                    .responseBody!!

            // then
            assertThat(response.status).isEqualTo("UP")
            assertThat(response.components).containsEntry("db", HealthComponent(status = "UP"))
        }

        private data class HealthResponse(
            val status: String,
            val components: Map<String, HealthComponent>,
        )

        private data class HealthComponent(
            val status: String,
        )
    }
