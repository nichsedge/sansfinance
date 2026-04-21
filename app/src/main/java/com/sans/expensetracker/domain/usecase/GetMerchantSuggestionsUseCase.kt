package com.sans.expensetracker.domain.usecase

import com.sans.expensetracker.domain.repository.ExpenseRepository
import javax.inject.Inject

class GetMerchantSuggestionsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        return repository.getMerchantSuggestions(query)
    }
}
