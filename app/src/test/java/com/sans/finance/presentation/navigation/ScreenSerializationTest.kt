package com.sans.finance.presentation.navigation

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class ScreenSerializationTest {

    private val json = Json {
        encodeDefaults = true
        // Navigation uses a specific configuration internally, 
        // but for general safety, we test with standard polymorphic serialization.
        classDiscriminator = "type"
    }

    @OptIn(InternalSerializationApi::class)
    @Test
    fun verifyAllScreenSubclassesAreTested() {
        val testedScreens = getTestScreens()
        val testedClasses = testedScreens.map { it::class }.toSet()
        
        val screenSubclasses = Screen::class.sealedSubclasses
        
        val missingSubclasses = screenSubclasses.filter { subclass ->
            testedClasses.none { it.isSubclassOf(subclass) }
        }
        
        assertTrue(
            "The following Screen subclasses are not covered by tests: ${missingSubclasses.map { it.simpleName }}",
            missingSubclasses.isEmpty()
        )
    }

    @Test
    fun serializeDeserialize_allScreens_roundTrip() {
        val screens = getTestScreens()

        screens.forEach { screen ->
            val serializer = Screen.serializer()
            val encoded = json.encodeToString(serializer, screen)
            val decoded = json.decodeFromString(serializer, encoded)
            assertEquals("Round-trip failed for ${screen::class.simpleName}", screen, decoded)
        }
    }

    private fun getTestScreens(): List<Screen> = listOf(
        Screen.Main,
        Screen.Dashboard,
        Screen.ExpenseList,
        Screen.AddTransaction,
        Screen.ExpenseDetail(expenseId = 77L),
        Screen.EditExpense(expenseId = 78L),
        Screen.Installments,
        Screen.TransactionStats,
        Screen.Settings,
        Screen.CategorySettings,
        Screen.TagSettings,
        Screen.AccountTypeSettings,
        Screen.RecurringExpenses,
        Screen.Accounts,
        Screen.Wealth,
        Screen.AccountStats,
        Screen.Portfolio,
        Screen.Goals,
        Screen.Budgets,
        Screen.WealthForecasting,
        Screen.DebtStrategist,
        Screen.Search,
        Screen.DataManagement,
        Screen.AiSettings,
        Screen.ReSyncDryRun,
        Screen.MonthlyReview(monthOffset = -2),
        Screen.MonthlyReview(monthOffset = 0),
        Screen.MonthlyReview(monthOffset = 12)
    )
}
