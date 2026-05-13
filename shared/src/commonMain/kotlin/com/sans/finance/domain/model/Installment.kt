package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Installment(
    val id: Long = 0,
    val expenseId: Long,
    val totalAmount: Long,
    val monthlyPayment: Long,
    val durationMonths: Int,
    val remainingBalance: Long,
    val nextDueDate: Long,
    val status: String = "Active",
    val createdAt: Long = System.currentTimeMillis(),
    val expenseName: String? = null,
    val expenseDate: Long? = null
)
