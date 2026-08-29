package com.krystianwitek.couponredemptionservice.coupon.infrastructure

import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionOperations

internal class FakeTransactionOperations : TransactionOperations {
    override fun <T> execute(action: TransactionCallback<T>): T = action.doInTransaction(SimpleTransactionStatus())
}
