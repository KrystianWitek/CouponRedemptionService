package com.krystianwitek.couponredemptionservice

import com.krystianwitek.couponredemptionservice.config.jsonMapper

fun Any.toJson(): String = jsonMapper.writeValueAsString(this)

inline fun <reified T> String.toObject(): T = jsonMapper.readValue(this, T::class.java)
