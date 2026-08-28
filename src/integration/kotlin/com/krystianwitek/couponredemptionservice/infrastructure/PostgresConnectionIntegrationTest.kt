package com.krystianwitek.couponredemptionservice.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@IntegrationTest
internal class PostgresConnectionIntegrationTest
    @Autowired
    constructor(
        private val jdbcTemplate: JdbcTemplate,
    ) {
        @Test
        fun `should connect to PostgreSQL container`() {
            // when
            val result = jdbcTemplate.queryForObject("SELECT 1", Int::class.java)

            // then
            assertThat(result).isEqualTo(1)
        }
    }
