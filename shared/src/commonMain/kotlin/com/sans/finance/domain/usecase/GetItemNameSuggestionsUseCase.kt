package com.sans.finance.domain.usecase

import com.sans.finance.domain.repository.ExpenseRepository
import javax.inject.Inject

class GetItemNameSuggestionsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        return repository.getNoteSuggestions(query)
    }
}
