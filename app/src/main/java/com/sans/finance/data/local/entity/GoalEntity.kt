package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val targetType: String = "TOTAL", // TOTAL, CATEGORY, ASSET_CLASS
    val targetName: String? = null,   // Specific category or asset class name
    val currency: String = "IDR",
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
