package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: Long = 0,
    val date: Long,
    val title: String,
    val amount: Long,
    val categoryId: Long,
    val isRecurring: Boolean = false,
    val isInstallment: Boolean = false,
    val recurrenceInterval: String? = null,
    val nextDueDate: Long? = null,
    val isInstallmentPayment: Boolean = false,
    val installmentMonth: Int = 0,
    val installmentTotalMonths: Int = 0,
    val status: String = "Paid",
    val details: String? = null,
    val accountId: Long = 1,
    val toAccountId: Long? = null,
    val type: String = "EXPENSE",
    val tags: List<String> = emptyList(),
    val currency: String = "USD",
    // Installment specific fields
    val totalPaid: Long = 0L,
    val remainingBalance: Long = 0L,
    val monthlyPayment: Long = 0L,
    val categoryName: String? = null,
    val categoryIcon: String? = null
)
