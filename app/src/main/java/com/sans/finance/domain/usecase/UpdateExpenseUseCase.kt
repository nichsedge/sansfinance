package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository,
    private val createInstallmentPlanUseCase: CreateInstallmentPlanUseCase
) {
    suspend operator fun invoke(expense: Expense, durationMonths: Int? = null) {
        val oldExpense = repository.getExpenseById(expense.id)
        repository.updateExpense(expense)

        // Handle installment transitions
        if (oldExpense?.isInstallment == true && !expense.isInstallment) {
            // Case: Installment -> Regular
            installmentRepository.deleteInstallmentByExpenseId(expense.id)
        } else if (expense.isInstallment) {
            // Case: Either already installment or switching to it
            // We delete existing and re-create to keep it simple and avoid duplicates or inconsistent states
            if (durationMonths != null && durationMonths > 0) {
                installmentRepository.deleteInstallmentByExpenseId(expense.id)
                createInstallmentPlanUseCase(
                    expenseId = expense.id,
                    totalAmount = expense.amount,
                    durationMonths = durationMonths,
                    startDate = expense.date
                )
            }
        }
    }
}
