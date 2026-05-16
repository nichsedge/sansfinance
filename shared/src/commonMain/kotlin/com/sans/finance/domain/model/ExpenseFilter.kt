package com.sans.finance.domain.model

data class ExpenseFilter(
    val query: String = "",
    val categoryIds: Set<Long> = emptySet(),
    val accountIds: Set<Long> = emptySet(),
    val since: Long = 0L,
    val until: Long = Long.MAX_VALUE,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val tags: Set<String> = emptySet(),
    val types: Set<String> = emptySet()
)
