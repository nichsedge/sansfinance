package com.sans.finance.data.local.dao

data class CategoryTotal(
    val category: String,
    val totalIdr: Double,
    val totalUsd: Double
)

data class AssetClassTotal(
    val assetClass: String,
    val totalIdr: Double
)

data class SnapshotTotal(
    val snapshot_date: Long,
    val totalIdr: Double,
    val totalUsd: Double
)
