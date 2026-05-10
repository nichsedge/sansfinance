package com.sans.finance.domain.model

data class InstallmentItem(
    val id: Long,
    val installmentId: Long,
    val amount: Long,
    val dueDate: Long,
    val status: String,
    val monthNumber: Int
)
