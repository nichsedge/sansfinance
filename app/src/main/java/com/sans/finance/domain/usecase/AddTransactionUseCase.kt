package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.TagRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val tagRepository: TagRepository,
    private val accountRepository: AccountRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase? = null
) {
    suspend operator fun invoke(expense: Expense): Long {
        val expenseId = repository.insertExpense(expense)

        // Sync tags
        if (expense.tags.isNotEmpty()) {
            tagRepository.syncTagsForExpense(expenseId, expense.tags)
        }

        // Adjust account balances
        if (!expense.isInstallment) {
            if (expense.type == "TRANSFER") {
                val fromAccount = accountRepository.getAccountById(expense.accountId)
                val fromDelta = if (fromAccount != null && fromAccount.currency.isNotBlank() && expense.currency.isNotBlank() && fromAccount.currency != expense.currency && convertCurrencyUseCase != null) {
                    convertCurrencyUseCase(expense.amount, expense.currency, fromAccount.currency, expense.date)
                } else {
                    expense.amount
                }
                accountRepository.updateBalance(expense.accountId, -fromDelta)

                expense.toAccountId?.let { toId ->
                    val toAccount = accountRepository.getAccountById(toId)
                    val toDelta = if (toAccount != null && toAccount.currency.isNotBlank() && expense.currency.isNotBlank() && toAccount.currency != expense.currency && convertCurrencyUseCase != null) {
                        convertCurrencyUseCase(expense.amount, expense.currency, toAccount.currency, expense.date)
                    } else {
                        expense.amount
                    }
                    accountRepository.updateBalance(toId, toDelta)
                }
            } else {
                val account = accountRepository.getAccountById(expense.accountId)
                val effectiveAmount = if (account != null && account.currency.isNotBlank() && expense.currency.isNotBlank() && account.currency != expense.currency && convertCurrencyUseCase != null) {
                    convertCurrencyUseCase(expense.amount, expense.currency, account.currency, expense.date)
                } else {
                    expense.amount
                }
                val isIncome = expense.type == "INCOME"
                val delta = if (isIncome) effectiveAmount else -effectiveAmount
                accountRepository.updateBalance(expense.accountId, delta)
            }
        }

        return expenseId
    }
}
