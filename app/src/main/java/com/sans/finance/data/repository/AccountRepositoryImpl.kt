package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.AccountDao
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class AccountRepositoryImpl(
    private val dao: AccountDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<AccountEntity>> = dao.getAllAccounts()
    override suspend fun getAccountById(id: Long): AccountEntity? = dao.getAccountById(id)
    override suspend fun insertAccount(account: AccountEntity): Long = dao.insertAccount(account)
    override suspend fun updateAccount(account: AccountEntity) = dao.updateAccount(account)
    override suspend fun deleteAccountById(id: Long) = dao.deleteAccountById(id)
}
