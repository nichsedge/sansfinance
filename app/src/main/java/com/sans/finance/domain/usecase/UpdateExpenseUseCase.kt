package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository,
    private val accountRepository: AccountRepository,
    private val tagRepository: TagRepository,
    private val createInstallmentPlanUseCase: CreateInstallmentPlanUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase? = null
) {
    suspend operator fun invoke(expense: Expense, durationMonths: Int? = null) {
        val oldExpense = repository.getExpenseById(expense.id) ?: return

        // Reverse old balance effect
        adjustBalance(oldExpense, isReverse = true)

        // Apply new balance effect
        adjustBalance(expense, isReverse = false)

        // Sync tags (if not synthetic payment which might not have its own tags stored in expense_tag_ref)
        if (!expense.isInstallmentPayment) {
            tagRepository.syncTagsForExpense(expense.id, expense.tags)
        }

        repository.updateExpense(expense)

        // Handle installment transitions for parent transactions
        if (!expense.isInstallmentPayment) {
            if (oldExpense.isInstallment && !expense.isInstallment) {
                installmentRepository.deleteInstallmentByExpenseId(expense.id)
            } else if (expense.isInstallment) {
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

    private suspend fun adjustBalance(expense: Expense, isReverse: Boolean) {
        if (expense.isInstallmentPayment) {
            // For installment items, we only care if they are "Paid"
            if (expense.status == "Paid") {
                val account = accountRepository.getAccountById(expense.accountId)
                val effectiveAmount = if (account != null && account.currency.isNotBlank() && expense.currency.isNotBlank() && account.currency != expense.currency && convertCurrencyUseCase != null) {
                    convertCurrencyUseCase(expense.amount, expense.currency, account.currency, expense.date)
                } else {
                    expense.amount
                }
                val delta = if (isReverse) effectiveAmount else -effectiveAmount
                accountRepository.updateBalance(expense.accountId, delta)
            }
            return
        }

        // For regular transactions
        // Note: Anchor installments (isInstallment = true) usually shouldn't affect balance if items do.
        // But we follow existing repository logic which was applying it.
        // If we want to fix double counting, we should check it here.
        if (expense.isInstallment) return // Fix double counting: anchor doesn't affect balance

        if (expense.type == "TRANSFER") {
            val multiplier = if (isReverse) 1 else -1
            val fromAccount = accountRepository.getAccountById(expense.accountId)
            val fromAmount = if (fromAccount != null && fromAccount.currency.isNotBlank() && expense.currency.isNotBlank() && fromAccount.currency != expense.currency && convertCurrencyUseCase != null) {
                convertCurrencyUseCase(expense.amount, expense.currency, fromAccount.currency, expense.date)
            } else {
                expense.amount
            }
            accountRepository.updateBalance(expense.accountId, fromAmount * multiplier)

            expense.toAccountId?.let { toId ->
                val toAccount = accountRepository.getAccountById(toId)
                val toAmount = if (toAccount != null && toAccount.currency.isNotBlank() && expense.currency.isNotBlank() && toAccount.currency != expense.currency && convertCurrencyUseCase != null) {
                    convertCurrencyUseCase(expense.amount, expense.currency, toAccount.currency, expense.date)
                } else {
                    expense.amount
                }
                accountRepository.updateBalance(toId, toAmount * -multiplier)
            }
        } else {
            val isIncome = expense.type == "INCOME"
            val multiplier = if (isReverse) -1 else 1
            val account = accountRepository.getAccountById(expense.accountId)
            val effectiveAmount = if (account != null && account.currency.isNotBlank() && expense.currency.isNotBlank() && account.currency != expense.currency && convertCurrencyUseCase != null) {
                convertCurrencyUseCase(expense.amount, expense.currency, account.currency, expense.date)
            } else {
                expense.amount
            }
            val delta = if (isIncome) effectiveAmount * multiplier else -effectiveAmount * multiplier
            accountRepository.updateBalance(expense.accountId, delta)
        }
    }
}
