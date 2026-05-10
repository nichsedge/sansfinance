package com.sans.finance.domain.model

data class Expense(
    val id: Long = 0,
    val date: Long,
    val itemName: String,
    val amount: Long,
    val categoryId: Long,
    val isRecurring: Boolean = false,
    val isInstallment: Boolean = false,
    val recurrenceInterval: String? = null,
    val nextDueDate: Long? = null,
    val isInstallmentPayment: Boolean = false,
    val merchant: String? = null,
    val accountId: Long = 1,
    val type: String = "EXPENSE",
    val tags: List<String> = emptyList(),
    val quantity: Int = 1,
    // Installment specific fields
    val totalPaid: Long = 0L,
    val remainingBalance: Long = 0L,
    val monthlyPayment: Long = 0L
)
