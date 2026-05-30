package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sans.finance.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY orderIndex ASC, id ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE isVisible = 1 ORDER BY orderIndex ASC, id ASC")
    fun getVisibleTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTag(tag: TagEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTags(tags: List<TagEntity>)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM expense_tag_ref)")
    suspend fun deleteOrphanedTags()
}
