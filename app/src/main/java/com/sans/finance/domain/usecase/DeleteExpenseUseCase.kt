package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository,
    private val accountRepository: AccountRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase? = null
) {
    suspend operator fun invoke(expense: Expense, deleteEntirePlan: Boolean = false) {
        // Reverse balance effect
        adjustBalance(expense, isReverse = true)

        if (deleteEntirePlan && expense.isInstallment) {
            expenseRepository.deleteExpense(expense)
        } else if (expense.isInstallmentPayment) {
            expenseRepository.deleteExpense(expense)
        } else {
            expenseRepository.deleteExpense(expense)
        }
    }

    private suspend fun adjustBalance(expense: Expense, isReverse: Boolean) {
        if (expense.isInstallmentPayment) {
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

        if (expense.isInstallment) return // Anchor doesn't affect balance

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
