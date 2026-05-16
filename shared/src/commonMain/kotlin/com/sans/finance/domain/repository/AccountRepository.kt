package com.sans.finance.domain.repository

import com.sans.finance.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun countAccountsByType(typeName: String): Int
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun renameTypeForAccounts(oldType: String, newType: String)
    suspend fun deleteAccountById(id: Long)
}
