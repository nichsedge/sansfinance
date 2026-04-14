package com.sans.expensetracker.data.util

import android.content.Context
import android.util.Log
import com.sans.expensetracker.data.local.entity.ExpenseEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

object CsvParser {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun parse(context: Context): List<ExpenseEntity> {
        val expenses = mutableListOf<ExpenseEntity>()
        val assetManager = context.assets

        try {
            val inputStream = assetManager.open("seed_transactions.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val row = parseCsvLine(line)
                if (row.size >= 4) {
                    val entity = mapRowToEntity(row)
                    if (entity != null) {
                        expenses.add(entity)
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CsvParser", "Error parsing CSV file", e)
        }

        return expenses
    }

    private fun parseCsvLine(line: String): List<String> {
        // Simple CSV parser that handles quoted strings with commas
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when (char) {
                '\"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }

                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun mapRowToEntity(row: List<String>): ExpenseEntity? {
        return try {
            // date,source,store,item_name,qty,item_price,order_total,status
            val dateStr = row.getOrNull(0) ?: return null
            val date = try {
                dateFormat.parse(dateStr)?.time
            } catch (e: Exception) {
                null
            } ?: System.currentTimeMillis()

            val platform = row.getOrNull(1)?.ifEmpty { null }
            val merchant = row.getOrNull(2)?.ifEmpty { null }
            val itemName = row.getOrNull(3)
            if (itemName.isNullOrEmpty()) return null

            val qty = row.getOrNull(4)?.toIntOrNull() ?: 1

            val originalPrice = parsePriceToCents(row.getOrNull(5) ?: "")
            val finalPrice = parsePriceToCents(row.getOrNull(6) ?: "")
            val status = row.getOrNull(7)?.ifEmpty { "Completed" } ?: "Completed"
            val isInstallment = row.getOrNull(8) == "1"

            val categoryId = getCategoryIdForPrompt(itemName, merchant)

            ExpenseEntity(
                date = date,
                platform = platform,
                merchant = merchant,
                itemName = itemName,
                quantity = qty,
                originalPrice = originalPrice,
                finalPrice = finalPrice,
                categoryId = categoryId,
                status = status,
                isRecurring = false,
                isInstallment = isInstallment
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePriceToCents(priceStr: String): Long {
        if (priceStr.isEmpty()) return 0L
        // Remove Rp, commas, quotes and spaces
        val clean = priceStr.replace("Rp", "")
            .replace(",", "")
            .replace("\"", "")
            .replace(".", "") // Some might have . instead of ,
            .trim()

        return clean.toLongOrNull()?.let { it * 100 } ?: 0L
    }

    private fun getCategoryIdForPrompt(itemName: String, merchant: String?): Long {
        val text = (itemName + " " + (merchant ?: "")).lowercase()
        return when {
            // Health & Personal Care (Category 2)
            containsAny(
                text,
                "sehat",
                "vitamin",
                "madu",
                "kesehatan",
                "obat",
                "creatine",
                "haid",
                "perut",
                "kemiri",
                "emina",
                "skincare",
                "masker",
                "whey",
                "fitness",
                "gym",
                "hospital",
                "doctor",
                "apotek",
                "suplemen"
            ) -> 2

            // Transport (Category 4)
            containsAny(
                text,
                "bensin",
                "grab",
                "gojek",
                "transport",
                "parkir",
                "commute",
                "toll",
                "go-jek",
                "perjalanan",
                "maxim",
                "fuel",
                "pertamina",
                "bus",
                "train",
                "kereta",
                "plane",
                "pesawat",
                "tiket",
                "travel"
            ) -> 4

            // Food & Drinks (Category 1)
            containsAny(
                text,
                "makan",
                "teh",
                "coffee",
                "kopi",
                "nasi",
                "bakso",
                "oat",
                "gandum",
                "susu",
                "dancow",
                "kacang",
                "chickpea",
                "strawberry",
                "restaurant",
                "warung",
                "ayam",
                "lele",
                "burger",
                "pizza",
                "snack",
                "camilan",
                "chips",
                "milk",
                "drink",
                "minum",
                "cola",
                "fanta",
                "sprite",
                "jus",
                "juice",
                "buah",
                "fruit",
                "cake",
                "bakery",
                "roti"
            ) -> 1

            // Shopping & Electronics & Home (Category 3)
            containsAny(
                text,
                "belanja",
                "shopee",
                "tokopedia",
                "tiktok",
                "baju",
                "celana",
                "sepatu",
                "kacamata",
                "helm",
                "case",
                "mouse",
                "keyboard",
                "meja",
                "strap",
                "jaket",
                "hanger",
                "spatula",
                "wajan",
                "tas",
                "buku",
                "pen",
                "powerbank",
                "xiaomi",
                "redmi",
                "tcl",
                "tv",
                "air fryer",
                "panci",
                "lampu",
                "kursi",
                "sofa",
                "bantal",
                "sprei",
                "tws",
                "headphone",
                "koper",
                "obeng",
                "sandal",
                "ikat pinggang",
                "sabuk",
                "belt",
                "kaos",
                "shirt",
                "jersey",
                "polos",
                "ram",
                "laptop",
                "ryzen",
                "hdd",
                "ssd",
                "memory",
                "monitor",
                "cpu",
                "gpu",
                "fan",
                "soft start",
                "stop kontak",
                "colokan",
                "kabel",
                "jack",
                "pria",
                "wanita",
                "unisex",
                "cotton",
                "combed",
                "bamboo",
                "fashion",
                "hp",
                "phone",
                "baterai",
                "battery",
                "handgrip",
                "parfum",
                "jam tangan",
                "watch",
                "gelang",
                "anting",
                "elektronik",
                "gadget",
                "accessories",
                "aksesoris"
            ) -> 3

            // Subscriptions & Bills (Category 5)
            containsAny(
                text,
                "pulsa",
                "kuota",
                "data",
                "inject",
                "tri",
                "xl",
                "indosat",
                "smartfren",
                "wifi",
                "modem",
                "spotify",
                "netflix",
                "youtube",
                "bill",
                "invoice",
                "internet",
                "hosting",
                "software",
                "license",
                "activation",
                "visio",
                "office",
                "streaming",
                "subscription",
                "berlangganan"
            ) -> 5

            else -> 6 // Others
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        for (keyword in keywords) {
            if (text.contains(keyword)) return true
        }
        return false
    }
}
