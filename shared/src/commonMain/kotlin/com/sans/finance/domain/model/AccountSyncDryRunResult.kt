package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountSyncDryRunResult(
    val accountId: Long,
    val accountName: String,
    val currentBalance: Long,
    val calculatedBalance: Long,
    val currency: String
) {
    val delta: Long get() = calculatedBalance - currentBalance
    val isDifferenceExist: Boolean get() = delta != 0L
}
