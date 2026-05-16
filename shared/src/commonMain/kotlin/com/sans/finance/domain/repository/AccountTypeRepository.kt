package com.sans.finance.domain.repository

import com.sans.finance.data.local.entity.AccountTypeEntity
import kotlinx.coroutines.flow.Flow

interface AccountTypeRepository {
    fun getAllAccountTypes(): Flow<List<AccountTypeEntity>>
    suspend fun getAccountTypeByName(name: String): AccountTypeEntity?
    suspend fun insertAccountType(accountType: AccountTypeEntity): Long
    suspend fun updateAccountType(accountType: AccountTypeEntity)
    suspend fun deleteAccountType(accountType: AccountTypeEntity)
    suspend fun deleteAccountTypeById(id: Long)
}
