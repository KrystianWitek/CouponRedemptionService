package com.krystianwitek.couponredemptionservice.infrastructure

import com.krystianwitek.couponredemptionservice.infrastructure.config.PostgresTestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PostgresTestConfiguration::class)
internal annotation class IntegrationTest
