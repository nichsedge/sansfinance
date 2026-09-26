package com.sans.finance.data.repository

import android.content.Context
import com.sans.finance.data.local.dao.AccountDao
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.presentation.widget.FinancialSummaryWidgetProvider
import kotlinx.coroutines.flow.Flow

class AccountRepositoryImpl(
    private val dao: AccountDao,
    private val context: Context? = null
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<AccountEntity>> = dao.getAllAccounts()
    override suspend fun getAccountById(id: Long): AccountEntity? = dao.getAccountById(id)
    override suspend fun countAccountsByType(typeName: String): Int = dao.countAccountsByType(typeName)
    override suspend fun insertAccount(account: AccountEntity): Long {
        val id = dao.insertAccount(account)
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
        return id
    }
    override suspend fun updateAccount(account: AccountEntity) {
        dao.updateAccount(account)
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
    }
    override suspend fun updateBalance(accountId: Long, delta: Long) {
        dao.adjustBalance(accountId, delta, System.currentTimeMillis())
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
    }
    override suspend fun renameTypeForAccounts(oldType: String, newType: String) {
        dao.renameTypeForAccounts(oldType, newType, System.currentTimeMillis())
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
    }
    override suspend fun deleteAccountById(id: Long) {
        dao.deleteAccountById(id)
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
    }
}
