package com.sans.finance.domain.usecase

import com.sans.finance.domain.repository.ExpenseRepository
import java.util.Calendar
import javax.inject.Inject

class GetFrequencyBasedSuggestionsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(): List<String> {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-6 starting from Sunday
        
        val daySuggestions = repository.getTopFrequentNotesByDay(dayOfWeek, 5)
        if (daySuggestions.isNotEmpty()) return daySuggestions
        
        return repository.getTopFrequentNotes(5)
    }
}
