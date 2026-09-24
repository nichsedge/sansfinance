package com.sans.finance.data.ai

import android.util.Log
import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiAssistantResponse
import com.sans.finance.domain.model.AiTransactionProposal
import com.sans.finance.domain.model.CategorySummary
import org.json.JSONArray
import org.json.JSONObject
import java.util.Stack

object AiJsonParser {

    private const val TAG = "AiJsonParser"

    fun parseAssistantResponse(
        rawText: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>
    ): AiAssistantResponse {
        val cleanText = sanitizeModelOutput(rawText)
        val jsonCandidate = extractJsonString(cleanText)

        // Try direct org.json parsing first
        var rootObj = tryParseJsonObject(jsonCandidate)

        // If direct parse failed, try repairing truncated JSON
        if (rootObj == null) {
            val repaired = repairTruncatedJson(jsonCandidate)
            rootObj = tryParseJsonObject(repaired)
        }

        if (rootObj != null) {
            val reply = rootObj.optString("reply").takeIf { it.isNotBlank() }
                ?: "Berikut transaksi yang berhasil dideteksi:"

            val proposals = mutableListOf<AiTransactionProposal>()

            val proposalsArr = rootObj.optJSONArray("proposals")
                ?: rootObj.optJSONArray("transactions")
                ?: rootObj.optJSONArray("items")

            if (proposalsArr != null) {
                for (i in 0 until proposalsArr.length()) {
                    val item = proposalsArr.optJSONObject(i) ?: continue
                    parseProposalObject(item, accounts, categories)?.let { proposals.add(it) }
                }
            } else {
                // Check single proposal object
                rootObj.optJSONObject("proposal")?.let { single ->
                    parseProposalObject(single, accounts, categories)?.let { proposals.add(it) }
                }
            }

            // If proposals were parsed successfully, return them
            if (proposals.isNotEmpty()) {
                return AiAssistantResponse(reply = reply, proposals = proposals)
            }

            // If rootObj had no proposals array or it was empty, check if regex can find proposals
            val fallbackProposals = extractProposalsViaRegex(jsonCandidate, accounts, categories)
            if (fallbackProposals.isNotEmpty()) {
                return AiAssistantResponse(reply = reply, proposals = fallbackProposals)
            }

            return AiAssistantResponse(reply = reply, proposals = emptyList())
        }

        // If rootObj couldn't be parsed at all, attempt regex extraction of proposals
        val regexProposals = extractProposalsViaRegex(rawText, accounts, categories)
        if (regexProposals.isNotEmpty()) {
            val fallbackReply = extractReplyString(rawText) ?: "Berikut transaksi yang berhasil dideteksi:"
            return AiAssistantResponse(reply = fallbackReply, proposals = regexProposals)
        }

        // Pure conversational response
        return AiAssistantResponse(
            reply = cleanText.ifBlank { rawText },
            proposals = emptyList()
        )
    }

    fun sanitizeModelOutput(text: String): String {
        return text
            .replace(Regex("""<think>[\s\S]*?</think>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<thought>[\s\S]*?</thought>""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun extractJsonString(raw: String): String {
        val trimmed = sanitizeModelOutput(raw)

        // Check for Markdown code block ```json ... ```
        val fencedMatch = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE).find(trimmed)
        if (fencedMatch != null) {
            val inner = fencedMatch.groupValues[1].trim()
            val start = inner.indexOf('{')
            val end = inner.lastIndexOf('}')
            if (start != -1 && end != -1 && end >= start) {
                return inner.substring(start, end + 1)
            }
            if (start != -1) {
                return inner.substring(start)
            }
            return inner
        }

        // Unfenced: find outermost {
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end >= start) {
            return trimmed.substring(start, end + 1)
        }
        if (start != -1) {
            return trimmed.substring(start)
        }

        return trimmed
    }

    fun repairTruncatedJson(input: String): String {
        val text = input.trim()
        if (text.isEmpty() || !text.startsWith('{')) return text

        val stack = Stack<Char>()
        var inString = false
        var isEscaped = false
        val sb = StringBuilder()

        for (ch in text) {
            sb.append(ch)
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (ch == '\\') {
                isEscaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == '{' || ch == '[') {
                    stack.push(ch)
                } else if (ch == '}' && stack.isNotEmpty() && stack.peek() == '{') {
                    stack.pop()
                } else if (ch == ']' && stack.isNotEmpty() && stack.peek() == '[') {
                    stack.pop()
                }
            }
        }

        // If truncated inside string, close the string
        if (inString) {
            sb.append('"')
        }

        var repaired = sb.toString().trim()

        // Strip trailing incomplete key or comma: e.g. ", \"key\":" or ","
        repaired = repaired.replace(Regex(""",\s*"[^"]*"\s*:\s*$"""), "")
        repaired = repaired.replace(Regex(""",\s*"[^"]*"\s*$"""), "")
        repaired = repaired.replace(Regex(""",\s*$"""), "")

        // Close all remaining unclosed brackets and braces
        val closingSb = StringBuilder(repaired)
        while (stack.isNotEmpty()) {
            when (stack.pop()) {
                '{' -> closingSb.append('}')
                '[' -> closingSb.append(']')
            }
        }

        return closingSb.toString()
    }

    private fun tryParseJsonObject(str: String): JSONObject? {
        return runCatching { JSONObject(str) }.getOrNull()
    }

    private fun parseProposalObject(
        obj: JSONObject,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>
    ): AiTransactionProposal? {
        val title = obj.optString("title").takeIf { it.isNotBlank() } ?: "Transaksi Baru"

        val rawAmount = when {
            obj.has("amount") -> {
                val opt = obj.opt("amount")
                when (opt) {
                    is Number -> opt.toDouble()
                    is String -> parseFlexibleAmount(opt)
                    null -> 0.0
                    else -> 0.0
                }
            }
            else -> 0.0
        }

        if (rawAmount <= 0.0 && title == "Transaksi Baru") return null

        val amountInCents = kotlin.math.round(rawAmount * 100.0).toLong()
        val type = obj.optString("type", "EXPENSE").uppercase()

        val rawDate = obj.optLong("date", 0L)
        val now = System.currentTimeMillis()
        val date = when {
            rawDate <= 0L -> now
            rawDate == 1726000000000L -> now // Legacy prompt dummy date (10 Sep 2024)
            rawDate < 100_000_000_000L -> rawDate * 1000L // 10-digit epoch timestamp in seconds -> ms
            else -> rawDate
        }

        val accountIdRaw = obj.optLong("accountId").takeIf { it > 0 }
        val accountNameRaw = obj.optString("accountName").takeIf { it.isNotBlank() }

        val defaultCashAccount = findDefaultCashAccount(accounts)

        val matchedAccount = when {
            accountIdRaw != null -> accounts.firstOrNull { it.id == accountIdRaw }
            !accountNameRaw.isNullOrBlank() -> {
                val cleanName = accountNameRaw.trim()
                val isCashHint = cleanName.equals("cash", ignoreCase = true) ||
                    cleanName.equals("tunai", ignoreCase = true) ||
                    cleanName.equals("dompet", ignoreCase = true) ||
                    cleanName.equals("wallet", ignoreCase = true)

                if (isCashHint && defaultCashAccount != null) {
                    defaultCashAccount
                } else {
                    accounts.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
                        ?: accounts.firstOrNull {
                            it.name.contains(cleanName, ignoreCase = true) ||
                                cleanName.contains(it.name, ignoreCase = true)
                        }
                        ?: accounts.firstOrNull { it.type.equals(cleanName, ignoreCase = true) }
                        ?: defaultCashAccount
                }
            }
            else -> defaultCashAccount
        } ?: defaultCashAccount

        val categoryIdRaw = obj.optLong("categoryId").takeIf { it > 0 }
        val categoryNameRaw = obj.optString("categoryName").takeIf { it.isNotBlank() }

        val matchedCategory = matchCategory(
            categoryIdRaw = categoryIdRaw,
            categoryNameRaw = categoryNameRaw,
            title = title,
            type = type,
            categories = categories
        )

        val notes = obj.optString("notes", "")
        val tags = mutableListOf<String>()
        val tagsArr = obj.optJSONArray("tags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val tag = tagsArr.optString(i)
                if (!tag.isNullOrBlank()) tags.add(tag)
            }
        }

        val resolvedAccountId = matchedAccount?.id ?: defaultCashAccount?.id ?: 1L
        val resolvedAccountName = matchedAccount?.name ?: defaultCashAccount?.name ?: "Cash"
        val proposalId = obj.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()

        return AiTransactionProposal(
            id = proposalId,
            title = title,
            amountInCents = amountInCents,
            type = type,
            date = date,
            accountId = resolvedAccountId,
            accountName = resolvedAccountName,
            categoryId = matchedCategory?.id ?: 1L,
            categoryName = matchedCategory?.name ?: "Umum",
            notes = notes,
            tags = tags
        )
    }

    fun findDefaultCashAccount(accounts: List<AccountSummary>): AccountSummary? {
        if (accounts.isEmpty()) return null

        // 1. Explicit Cash account type
        accounts.firstOrNull { it.type.equals("Cash", ignoreCase = true) }?.let { return it }

        // 2. Name contains cash / wallet / tunai / dompet
        accounts.firstOrNull { acc ->
            val n = acc.name.lowercase()
            n.contains("cash") || n.contains("wallet") || n.contains("tunai") || n.contains("dompet")
        }?.let { return it }

        // 3. Liquid non-liability, non-investment account (e.g. Bank Account / Checking)
        accounts.firstOrNull { acc ->
            val t = acc.type.lowercase()
            t !in listOf("investment", "credit card", "p2p lending") &&
                !acc.name.lowercase().contains("rdn")
        }?.let { return it }

        // 4. Any non-investment, non-liability
        accounts.firstOrNull { acc ->
            val t = acc.type.lowercase()
            t != "investment" && t != "credit card"
        }?.let { return it }

        // 5. Fallback to first available account
        return accounts.firstOrNull()
    }

    fun matchCategory(
        categoryIdRaw: Long?,
        categoryNameRaw: String?,
        title: String,
        type: String,
        categories: List<CategorySummary>
    ): CategorySummary? {
        if (categories.isEmpty()) return null

        // 1. Direct ID match
        if (categoryIdRaw != null && categoryIdRaw > 0) {
            categories.firstOrNull { it.id == categoryIdRaw }?.let { return it }
        }

        // 2. Exact or substring match on name
        if (!categoryNameRaw.isNullOrBlank()) {
            val clean = categoryNameRaw.trim()
            categories.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it }
            categories.firstOrNull {
                it.name.contains(clean, ignoreCase = true) || clean.contains(it.name, ignoreCase = true)
            }?.let { return it }

            // 3. Synonym / translation mapping (Indonesian -> English category names)
            val mappedCategoryName = mapIndonesianCategoryToEnglish(clean)
            if (mappedCategoryName != null) {
                categories.firstOrNull { it.name.equals(mappedCategoryName, ignoreCase = true) }?.let { return it }
            }
        }

        // 4. Try IndonesianMerchantClassifier on title
        val classification = com.sans.finance.core.util.IndonesianMerchantClassifier.classify(title)
        if (classification != null) {
            categories.firstOrNull { it.name.equals(classification.suggestedCategory, ignoreCase = true) }?.let { return it }
        }

        // 5. Fallback to first category matching type
        return categories.firstOrNull { it.type.equals(type, ignoreCase = true) }
            ?: categories.firstOrNull()
    }

    fun mapIndonesianCategoryToEnglish(raw: String): String? {
        val s = raw.lowercase().trim()
        return when {
            // Food & Beverage
            s in listOf("makanan", "makan", "kuliner", "f&b", "restoran", "resto", "warung", "warteg", "kafe", "cafe", "minuman", "kopi", "snack", "sarapan", "lunch", "dinner") ||
                s.contains("makan") || s.contains("kuliner") -> "Food"

            // Groceries & Shopping
            s in listOf("belanja", "shopping", "groceries", "supermarket", "minimarket", "sembako", "baju", "pakaian", "elektronik", "pasar") ||
                s.contains("belanja") || s.contains("grocer") -> "Shopping"

            // Utilities & Bills
            s in listOf("utilitas", "utility", "tagihan", "listrik", "pln", "air", "pdam", "internet", "wifi", "pulsa", "paket data", "kos", "kontrakan", "sewa", "bpjs") ||
                s.contains("tagihan") || s.contains("listrik") || s.contains("pulsa") -> "Utility"

            // Transport & Fuel
            s in listOf("transportasi", "transport", "bensin", "bbm", "spbu", "pertamina", "shell", "ojol", "taksi", "ojek", "krl", "mrt", "tol", "parkir", "kendaraan") ||
                s.contains("transport") || s.contains("bensin") -> "Transport"

            // Subscriptions
            s in listOf("langganan", "subscription", "subscriptions", "digital", "streaming") ||
                s.contains("langgan") || s.contains("subscri") -> "Subscriptions"

            // Entertainment
            s in listOf("hiburan", "entertainment", "rekreasi", "wisata", "liburan", "nonton", "bioskop", "cinema", "game", "gaming", "hobi") ||
                s.contains("hibur") || s.contains("wisata") -> "Entertainment"

            // Misc
            s in listOf("lain-lain", "lainnya", "misc", "miscellaneous", "pajak", "admin", "biaya admin", "fee", "sedekah", "donasi", "zakat") -> "Misc"

            // Salary / Active Income
            s in listOf("gaji", "salary", "payroll", "upah", "honor", "penghasilan", "tunjangan") ||
                s.contains("gaji") || s.contains("payroll") -> "Salary"

            // Business
            s in listOf("bisnis", "business", "usaha", "omset", "dagang", "jual", "penjualan", "freelance") ||
                s.contains("bisnis") || s.contains("usaha") -> "Business"

            // Bonus / Gifts
            s in listOf("bonus", "thr", "insentif", "hadiah", "reward", "cashback", "reimburse", "reimbursement", "komisi") ||
                s.contains("bonus") || s.contains("cashback") -> "Bonus"

            // Investments / Yields
            s in listOf("investasi", "investment", "investments", "kupon", "kupon sbn", "sbn", "dividen", "dividend", "yield", "bunga", "deposito", "reksadana", "saham", "crypto", "p2p", "staking") ||
                s.contains("invest") || s.contains("kupon") || s.contains("dividen") -> "Investments"

            else -> null
        }
    }

    fun parseFlexibleAmount(raw: String): Double {
        val clean = raw.trim()
            .replace("Rp", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace(" ", "")

        if (clean.isBlank()) return 0.0

        if (clean.contains('.') && clean.contains(',')) {
            val lastDot = clean.lastIndexOf('.')
            val lastComma = clean.lastIndexOf(',')
            return if (lastComma > lastDot) {
                clean.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
            } else {
                clean.replace(",", "").toDoubleOrNull() ?: 0.0
            }
        }

        if (clean.contains('.')) {
            val parts = clean.split('.')
            return if (parts.size > 2 || (parts.size == 2 && parts[1].length == 3)) {
                clean.replace(".", "").toDoubleOrNull() ?: 0.0
            } else {
                clean.toDoubleOrNull() ?: 0.0
            }
        }

        if (clean.contains(',')) {
            val parts = clean.split(',')
            return if (parts.size > 2 || (parts.size == 2 && parts[1].length == 3)) {
                clean.replace(",", "").toDoubleOrNull() ?: 0.0
            } else {
                clean.replace(",", ".").toDoubleOrNull() ?: 0.0
            }
        }

        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun extractReplyString(raw: String): String? {
        val match = Regex(""""reply"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(raw)
        return match?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")
    }

    private fun extractProposalsViaRegex(
        text: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>
    ): List<AiTransactionProposal> {
        val proposals = mutableListOf<AiTransactionProposal>()
        // Match individual JSON objects containing title and amount
        val objectRegex = Regex("""\{[^{}]*"title"[^{}]*"amount"[^{}]*\}""")
        objectRegex.findAll(text).forEach { match ->
            runCatching {
                val obj = JSONObject(match.value)
                parseProposalObject(obj, accounts, categories)?.let { proposals.add(it) }
            }
        }
        return proposals
    }
}
