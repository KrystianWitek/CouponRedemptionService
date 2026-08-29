package com.krystianwitek.couponredemptionservice.infrastructure

import com.krystianwitek.couponredemptionservice.infrastructure.config.PostgresTestConfiguration
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestConfiguration::class)
@ActiveProfiles("integration")
internal annotation class IntegrationTest
