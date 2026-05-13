package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class DetectRecurringPatternsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(): List<Expense> {
        val expenses = repository.getAllExpenses().first()
        
        // Group by normalized note and amount
        val groups = expenses.filter { 
            it.type == "EXPENSE" && !it.isRecurring && !it.isInstallment && !it.isInstallmentPayment 
        }.groupBy { 
            it.note.trim().lowercase() to it.amount 
        }
        
        return groups.filter { (_, list) ->
            if (list.size < 2) return@filter false
            
            // Check if they appear in different months
            val months = list.map { 
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.date
                cal.get(Calendar.MONTH) + cal.get(Calendar.YEAR) * 12
            }.distinct()
            
            months.size >= 2
        }.values.map { it.first() }
    }
}
