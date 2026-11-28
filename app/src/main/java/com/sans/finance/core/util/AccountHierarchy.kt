package com.sans.finance.core.util

import com.sans.finance.data.local.entity.AccountEntity

/**
 * Represents top-level accounts and their direct subaccounts grouped by parent account ID.
 */
data class AccountHierarchy(
    val topLevel: List<AccountEntity>,
    val childrenByParent: Map<Long, List<AccountEntity>>
)

/**
 * Groups accounts into top-level parents and their direct children without unsafe non-null casts.
 * Any account whose parent is null or absent from this list is treated as top-level.
 */
fun List<AccountEntity>.groupByParent(): AccountHierarchy {
    val accountIds = mapTo(HashSet(size)) { it.id }
    val childrenByParent = buildMap<Long, MutableList<AccountEntity>> {
        for (account in this@groupByParent) {
            val parentId = account.parentAccountId
            if (parentId != null && parentId in accountIds) {
                getOrPut(parentId) { mutableListOf() }.add(account)
            }
        }
    }
    val topLevel = filter { it.parentAccountId == null || it.parentAccountId !in accountIds }
    return AccountHierarchy(topLevel = topLevel, childrenByParent = childrenByParent)
}
