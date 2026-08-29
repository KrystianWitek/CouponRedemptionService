package com.krystianwitek.couponredemptionservice.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Configuration
class JacksonConfig {
    @Bean
    @Primary
    fun jsonMapper(): JsonMapper = jsonMapper
}

val jsonMapper: JsonMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .build()
