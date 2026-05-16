package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.AccountTypeDao
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.domain.repository.AccountTypeRepository
import kotlinx.coroutines.flow.Flow

class AccountTypeRepositoryImpl(
    private val dao: AccountTypeDao
) : AccountTypeRepository {
    override fun getAllAccountTypes(): Flow<List<AccountTypeEntity>> = dao.getAllAccountTypes()
    override suspend fun getAccountTypeByName(name: String): AccountTypeEntity? = dao.getAccountTypeByName(name)
    override suspend fun insertAccountType(accountType: AccountTypeEntity): Long = dao.insertAccountType(accountType)
    override suspend fun updateAccountType(accountType: AccountTypeEntity) = dao.updateAccountType(accountType)
    override suspend fun deleteAccountType(accountType: AccountTypeEntity) = dao.deleteAccountType(accountType)
    override suspend fun deleteAccountTypeById(id: Long) = dao.deleteAccountTypeById(id)
}
