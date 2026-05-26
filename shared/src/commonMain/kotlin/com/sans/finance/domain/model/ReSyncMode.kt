package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReSyncMode {
    TRANSACTIONS_AS_TRUTH, // Adjust account balance to match transactions (Current behavior)
    BALANCE_AS_TRUTH       // Add adjustment transactions to match account balance (New behavior)
}
