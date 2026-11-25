package com.sans.finance.domain.usecase

import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.util.CloudStorageSyncer
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

data class EmailSyncSummary(
    val insertedCount: Int,
    val duplicateCount: Int,
    val wasEmpty: Boolean
)

/**
 * Fetches staged email expenses from the Sync Gateway, deduplicates against recent local expenses,
 * persists new transactions via [AddTransactionUseCase] (updating account balances & widgets),
 * and acknowledges processed IDs.
 */
class SyncEmailExpensesUseCase @Inject constructor(
    private val db: AppDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) {
    suspend operator fun invoke(): Result<EmailSyncSummary> = withContext(Dispatchers.IO) {
        val prefs = userPreferencesRepository.userPreferences.first()
        val fetchResult = CloudStorageSyncer.fetchInboxExpenses(prefs)
        val items = fetchResult.getOrElse { return@withContext Result.failure(it) }

        if (items.isEmpty()) {
            userPreferencesRepository.setLastEmailSyncTime(System.currentTimeMillis())
            return@withContext Result.success(EmailSyncSummary(0, 0, wasEmpty = true))
        }

        val categories = db.categoryDao.getAllCategoriesSync()
        val accounts = db.accountDao.getAllAccountsOnce()
        val cashAccount = accounts.firstOrNull { it.type.equals("CASH", ignoreCase = true) }
            ?: accounts.firstOrNull { it.name.contains("cash", ignoreCase = true) || it.name.contains("wallet", ignoreCase = true) }
            ?: accounts.firstOrNull()

        val existingExpenses = db.expenseDao.getAllExpenseEntities()

        val toAck = mutableListOf<String>()
        var newInsertedCount = 0
        var duplicateCount = 0
        val now = System.currentTimeMillis()

        for (item in items) {
            val amountCents = BigDecimal.valueOf(item.amount)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()

            val itemTime = runCatching {
                OffsetDateTime.parse(item.receivedAt, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            }.getOrDefault(now)

            val txType = if (item.type.equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"

            val isDuplicate = existingExpenses.any { existing ->
                existing.amount == amountCents &&
                    existing.type.equals(txType, ignoreCase = true) &&
                    abs(existing.date - itemTime) < 15 * 60 * 1000L
            }

            if (isDuplicate) {
                duplicateCount++
                toAck.add(item.id)
            } else {
                val matchedCat = categories.firstOrNull { it.type == txType && it.name.equals(item.category, ignoreCase = true) }
                    ?: categories.firstOrNull { it.name.equals(item.category, ignoreCase = true) }
                    ?: categories.firstOrNull { it.type == txType && it.name.equals("Investments", ignoreCase = true) }
                    ?: categories.firstOrNull { it.type == txType && it.name.equals("Misc", ignoreCase = true) }
                    ?: categories.firstOrNull { it.type == txType }
                    ?: categories.firstOrNull()

                val matchedAcc = if (txType == "INCOME") {
                    cashAccount
                } else {
                    accounts.firstOrNull { acc ->
                        item.paymentMethod.contains(acc.name, ignoreCase = true) || acc.name.contains(item.paymentMethod, ignoreCase = true)
                    } ?: cashAccount
                }

                val expense = Expense(
                    date = itemTime,
                    title = item.merchant,
                    details = "Imported from email (${item.paymentMethod})",
                    amount = amountCents,
                    categoryId = matchedCat?.id ?: 1L,
                    accountId = matchedAcc?.id ?: 1L,
                    type = txType,
                    currency = item.currency,
                    status = "COMPLETED",
                    isRecurring = false
                )
                addTransactionUseCase(expense)
                newInsertedCount++
                toAck.add(item.id)
            }
        }

        if (toAck.isNotEmpty()) {
            CloudStorageSyncer.acknowledgeInboxExpenses(toAck, prefs)
        }
        userPreferencesRepository.setLastEmailSyncTime(System.currentTimeMillis())

        Result.success(EmailSyncSummary(newInsertedCount, duplicateCount, wasEmpty = false))
    }
}
