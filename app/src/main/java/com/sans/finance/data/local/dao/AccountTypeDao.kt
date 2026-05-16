package com.sans.finance.data.local.dao

import androidx.room.*
import com.sans.finance.data.local.entity.AccountTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountTypeDao {
    @Query("SELECT * FROM account_types ORDER BY display_order ASC, name ASC")
    fun getAllAccountTypes(): Flow<List<AccountTypeEntity>>

    @Query("SELECT * FROM account_types WHERE name = :name LIMIT 1")
    suspend fun getAccountTypeByName(name: String): AccountTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountType(accountType: AccountTypeEntity): Long

    @Update
    suspend fun updateAccountType(accountType: AccountTypeEntity)

    @Delete
    suspend fun deleteAccountType(accountType: AccountTypeEntity)

    @Query("DELETE FROM account_types WHERE id = :id")
    suspend fun deleteAccountTypeById(id: Long)
}
