package com.krystianwitek.couponservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CouponServiceApplication

fun main(args: Array<String>) {
	runApplication<CouponServiceApplication>(*args)
}
