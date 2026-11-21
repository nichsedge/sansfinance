package com.sans.finance.domain.usecase

import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.repository.CategoryRepository
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

data class BudgetSuggestion(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String?,
    val suggestedAmount: Long,
    val last3MonthsAverage: Long
)

class SuggestBudgetsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(): List<BudgetSuggestion> {
        val cal = CalendarUtils.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val currentMonthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, -3)
        val threeMonthsAgoStart = cal.timeInMillis

        // Get spending for last 3 full months
        val spending = expenseRepository.getSpendingByCategoryBetween(threeMonthsAgoStart, currentMonthStart).first()
        val categories = categoryRepository.getAllCategories().first()

        return spending.map { spent ->
            val avg = spent.totalAmount / 3
            // Round up to nearest 50k or 100k roughly if high, or just provide as is.
            // Let's suggest +10% buffer
            val suggested = (avg * 1.1).toLong()

            BudgetSuggestion(
                categoryId = spent.categoryId,
                categoryName = spent.categoryName,
                categoryIcon = spent.categoryIcon,
                suggestedAmount = suggested,
                last3MonthsAverage = avg
            )
        }.sortedByDescending { it.last3MonthsAverage }
    }
}
