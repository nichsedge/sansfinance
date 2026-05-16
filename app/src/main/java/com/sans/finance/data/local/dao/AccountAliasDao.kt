package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sans.finance.data.local.entity.AccountAliasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountAliasDao {
    @Query("SELECT * FROM account_aliases")
    fun getAllAliases(): Flow<List<AccountAliasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlias(alias: AccountAliasEntity)
}
