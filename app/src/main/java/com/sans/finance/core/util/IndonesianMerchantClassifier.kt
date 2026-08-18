package com.sans.finance.core.util

data class ClassificationResult(
    val normalizedMerchant: String,
    val suggestedCategory: String,
    val suggestedType: String = "EXPENSE",
    val tags: List<String> = emptyList()
)

object IndonesianMerchantClassifier {

    private data class Rule(
        val patterns: List<Regex>,
        val normalizedName: String,
        val category: String,
        val type: String = "EXPENSE",
        val tags: List<String> = emptyList()
    )

    private val rules = listOf(
        // Food & Dining
        Rule(
            patterns = listOf("gofood".toRegex(RegexOption.IGNORE_CASE), "grabfood".toRegex(RegexOption.IGNORE_CASE), "shopeefood".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Food Delivery",
            category = "Food",
            tags = listOf("food_delivery", "dining")
        ),
        Rule(
            patterns = listOf("starbucks".toRegex(RegexOption.IGNORE_CASE), "kopi kenangan".toRegex(RegexOption.IGNORE_CASE), "fore coffee".toRegex(RegexOption.IGNORE_CASE), "tomoro".toRegex(RegexOption.IGNORE_CASE), "janji jiwa".toRegex(RegexOption.IGNORE_CASE), "mixue".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Coffee & Beverage",
            category = "Food",
            tags = listOf("coffee", "drinks")
        ),
        Rule(
            patterns = listOf("mcdonald".toRegex(RegexOption.IGNORE_CASE), "mcd".toRegex(RegexOption.IGNORE_CASE), "kfc".toRegex(RegexOption.IGNORE_CASE), "burger king".toRegex(RegexOption.IGNORE_CASE), "hokben".toRegex(RegexOption.IGNORE_CASE), "solaria".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Fast Food & Dining",
            category = "Food",
            tags = listOf("dining_out")
        ),

        // Groceries & Daily Needs
        Rule(
            patterns = listOf("indomaret".toRegex(RegexOption.IGNORE_CASE), "alfamart".toRegex(RegexOption.IGNORE_CASE), "alfamidi".toRegex(RegexOption.IGNORE_CASE), "superindo".toRegex(RegexOption.IGNORE_CASE), "super indo".toRegex(RegexOption.IGNORE_CASE), "hypermart".toRegex(RegexOption.IGNORE_CASE), "grandlucky".toRegex(RegexOption.IGNORE_CASE), "sayurbox".toRegex(RegexOption.IGNORE_CASE), "astro".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Supermarket & Groceries",
            category = "Groceries",
            tags = listOf("groceries", "household")
        ),

        // Transport & Fuel
        Rule(
            patterns = listOf("goride".toRegex(RegexOption.IGNORE_CASE), "gocar".toRegex(RegexOption.IGNORE_CASE), "grabbike".toRegex(RegexOption.IGNORE_CASE), "grabcar".toRegex(RegexOption.IGNORE_CASE), "bluebird".toRegex(RegexOption.IGNORE_CASE), "maxim".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Ride Hailing",
            category = "Transport",
            tags = listOf("ride_hailing", "transport")
        ),
        Rule(
            patterns = listOf("krl".toRegex(RegexOption.IGNORE_CASE), "mrt".toRegex(RegexOption.IGNORE_CASE), "lrt".toRegex(RegexOption.IGNORE_CASE), "transjakarta".toRegex(RegexOption.IGNORE_CASE), "commuter".toRegex(RegexOption.IGNORE_CASE), "kereta api".toRegex(RegexOption.IGNORE_CASE), "kai".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Public Transit",
            category = "Transport",
            tags = listOf("public_transit", "transport")
        ),
        Rule(
            patterns = listOf("pertamina".toRegex(RegexOption.IGNORE_CASE), "spbu".toRegex(RegexOption.IGNORE_CASE), "shell".toRegex(RegexOption.IGNORE_CASE), "bp-akr".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Fuel & Gas",
            category = "Transport",
            tags = listOf("fuel", "vehicle")
        ),

        // Bills & Utilities
        Rule(
            patterns = listOf("pln".toRegex(RegexOption.IGNORE_CASE), "token listrik".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Electricity (PLN)",
            category = "Utility",
            tags = listOf("electricity", "bills")
        ),
        Rule(
            patterns = listOf("pdam".toRegex(RegexOption.IGNORE_CASE), "air bersih".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Water (PDAM)",
            category = "Utility",
            tags = listOf("water", "bills")
        ),
        Rule(
            patterns = listOf("telkomsel".toRegex(RegexOption.IGNORE_CASE), "indihome".toRegex(RegexOption.IGNORE_CASE), "myxl".toRegex(RegexOption.IGNORE_CASE), "xl axiata".toRegex(RegexOption.IGNORE_CASE), "by.u".toRegex(RegexOption.IGNORE_CASE), "tri".toRegex(RegexOption.IGNORE_CASE), "smartfren".toRegex(RegexOption.IGNORE_CASE), "biznet".toRegex(RegexOption.IGNORE_CASE), "first media".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Internet & Cellular",
            category = "Utility",
            tags = listOf("internet", "phone", "bills")
        ),
        Rule(
            patterns = listOf("bpjs".toRegex(RegexOption.IGNORE_CASE), "bpjs kesehatan".toRegex(RegexOption.IGNORE_CASE), "bpjs ketenagakerjaan".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "BPJS Healthcare",
            category = "Health",
            tags = listOf("insurance", "health")
        ),

        // Digital Subscriptions & Tech
        Rule(
            patterns = listOf("netflix".toRegex(RegexOption.IGNORE_CASE), "spotify".toRegex(RegexOption.IGNORE_CASE), "youtube premium".toRegex(RegexOption.IGNORE_CASE), "disney".toRegex(RegexOption.IGNORE_CASE), "app store".toRegex(RegexOption.IGNORE_CASE), "google play".toRegex(RegexOption.IGNORE_CASE), "icloud".toRegex(RegexOption.IGNORE_CASE), "openai".toRegex(RegexOption.IGNORE_CASE), "chatgpt".toRegex(RegexOption.IGNORE_CASE), "claude".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Digital Subscription",
            category = "Entertainment",
            tags = listOf("subscription", "digital")
        ),

        // Entertainment & Hobbies
        Rule(
            patterns = listOf("cinema xxi".toRegex(RegexOption.IGNORE_CASE), "cgv".toRegex(RegexOption.IGNORE_CASE), "cinepolis".toRegex(RegexOption.IGNORE_CASE), "steam".toRegex(RegexOption.IGNORE_CASE), "playstation".toRegex(RegexOption.IGNORE_CASE), "nintendo".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Movies & Gaming",
            category = "Entertainment",
            tags = listOf("movies", "gaming")
        ),

        // Income
        Rule(
            patterns = listOf("gaji".toRegex(RegexOption.IGNORE_CASE), "payroll".toRegex(RegexOption.IGNORE_CASE), "salary".toRegex(RegexOption.IGNORE_CASE), "upah".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Salary / Payroll",
            category = "Salary",
            type = "INCOME",
            tags = listOf("salary", "active_income")
        ),
        Rule(
            patterns = listOf("dividen".toRegex(RegexOption.IGNORE_CASE), "dividend".toRegex(RegexOption.IGNORE_CASE), "kupon sbn".toRegex(RegexOption.IGNORE_CASE), "imbal hasil".toRegex(RegexOption.IGNORE_CASE), "bunga deposito".toRegex(RegexOption.IGNORE_CASE)),
            normalizedName = "Investment Yield",
            category = "Investments",
            type = "INCOME",
            tags = listOf("passive_income", "dividend")
        )
    )

    fun classify(titleOrDescription: String): ClassificationResult? {
        val trimmed = titleOrDescription.trim()
        if (trimmed.isEmpty()) return null

        for (rule in rules) {
            if (rule.patterns.any { it.containsMatchIn(trimmed) }) {
                return ClassificationResult(
                    normalizedMerchant = rule.normalizedName,
                    suggestedCategory = rule.category,
                    suggestedType = rule.type,
                    tags = rule.tags
                )
            }
        }
        return null
    }
}
