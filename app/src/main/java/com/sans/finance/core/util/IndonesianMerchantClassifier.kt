package com.sans.finance.core.util

data class ClassificationResult(
    val normalizedMerchant: String,
    val suggestedCategory: String,
    val suggestedType: String = "EXPENSE",
    val tags: List<String> = emptyList()
)

object IndonesianMerchantClassifier {

    private data class Rule(
        val keywords: List<String>,
        val normalizedName: String,
        val category: String,
        val type: String = "EXPENSE",
        val tags: List<String> = emptyList()
    )

    private val rules = listOf(
        // Food & Dining
        Rule(
            keywords = listOf("gofood", "grabfood", "shopeefood"),
            normalizedName = "Food Delivery",
            category = "Food",
            tags = listOf("food_delivery", "dining")
        ),
        Rule(
            keywords = listOf("starbucks", "kopi kenangan", "fore coffee", "tomoro", "janji jiwa", "mixue"),
            normalizedName = "Coffee & Beverage",
            category = "Food",
            tags = listOf("coffee", "drinks")
        ),
        Rule(
            keywords = listOf("mcdonald", "mcd", "kfc", "burger king", "hokben", "solaria"),
            normalizedName = "Fast Food & Dining",
            category = "Food",
            tags = listOf("dining_out")
        ),

        // Groceries & Daily Needs
        Rule(
            keywords = listOf("indomaret", "alfamart", "alfamidi", "superindo", "super indo", "hypermart", "grandlucky", "sayurbox", "astro"),
            normalizedName = "Supermarket & Groceries",
            category = "Shopping",
            tags = listOf("groceries", "household")
        ),

        // Transport & Fuel
        Rule(
            keywords = listOf("goride", "gocar", "grabbike", "grabcar", "bluebird", "maxim"),
            normalizedName = "Ride Hailing",
            category = "Transport",
            tags = listOf("ride_hailing", "transport")
        ),
        Rule(
            keywords = listOf("krl", "mrt", "lrt", "transjakarta", "commuter", "kereta api", "kai"),
            normalizedName = "Public Transit",
            category = "Transport",
            tags = listOf("public_transit", "transport")
        ),
        Rule(
            keywords = listOf("pertamina", "spbu", "shell", "bp-akr"),
            normalizedName = "Fuel & Gas",
            category = "Transport",
            tags = listOf("fuel", "vehicle")
        ),

        // Bills & Utilities
        Rule(
            keywords = listOf("pln", "token listrik"),
            normalizedName = "Electricity (PLN)",
            category = "Utility",
            tags = listOf("electricity", "bills")
        ),
        Rule(
            keywords = listOf("pdam", "air bersih"),
            normalizedName = "Water (PDAM)",
            category = "Utility",
            tags = listOf("water", "bills")
        ),
        Rule(
            keywords = listOf("telkomsel", "indihome", "myxl", "xl axiata", "by.u", "tri", "smartfren", "biznet", "first media"),
            normalizedName = "Internet & Cellular",
            category = "Utility",
            tags = listOf("internet", "phone", "bills")
        ),
        Rule(
            keywords = listOf("bpjs", "bpjs kesehatan", "bpjs ketenagakerjaan"),
            normalizedName = "BPJS Healthcare",
            category = "Utility",
            tags = listOf("insurance", "health", "bills")
        ),

        // Digital Subscriptions & Tech
        Rule(
            keywords = listOf("netflix", "spotify", "youtube premium", "disney", "app store", "google play", "icloud", "openai", "chatgpt", "claude"),
            normalizedName = "Digital Subscription",
            category = "Subscriptions",
            tags = listOf("subscription", "digital")
        ),

        // Entertainment & Hobbies
        Rule(
            keywords = listOf("cinema xxi", "cgv", "cinepolis", "steam", "playstation", "nintendo"),
            normalizedName = "Movies & Gaming",
            category = "Entertainment",
            tags = listOf("movies", "gaming")
        ),

        // Income
        Rule(
            keywords = listOf("gaji", "payroll", "salary", "upah"),
            normalizedName = "Salary / Payroll",
            category = "Salary",
            type = "INCOME",
            tags = listOf("salary", "active_income")
        ),
        Rule(
            keywords = listOf("dividen", "dividend", "kupon sbn", "imbal hasil", "bunga deposito"),
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
            for (keyword in rule.keywords) {
                if (trimmed.contains(keyword, ignoreCase = true)) {
                    return ClassificationResult(
                        normalizedMerchant = rule.normalizedName,
                        suggestedCategory = rule.category,
                        suggestedType = rule.type,
                        tags = rule.tags
                    )
                }
            }
        }
        return null
    }
}
