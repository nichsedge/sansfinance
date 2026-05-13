package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository
) {
    suspend operator fun invoke(expense: Expense, deleteEntirePlan: Boolean = false) {
        if (deleteEntirePlan && expense.isInstallment) {
            // Delete the entire installment plan by deleting the anchor expense
            // Cascade delete will handle installments and items
            expenseRepository.deleteExpense(expense)
        } else if (expense.isInstallmentPayment) {
            // Delete just this payment (pseudo-expense)
            // In ExpenseRepositoryImpl, we'll handle this by marking the installment item as Pending
            expenseRepository.deleteExpense(expense)
        } else {
            // Regular expense or recurring definition
            expenseRepository.deleteExpense(expense)
        }
    }
}
