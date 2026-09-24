package com.sans.finance.data.ai

import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.CategorySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiJsonParserTest {

    private val testAccounts = listOf(
        AccountSummary(id = 21L, name = "BNI Taplus", type = "CHECKING", currency = "IDR"),
        AccountSummary(id = 1L, name = "Wallet", type = "CASH", currency = "IDR")
    )

    private val testCategories = listOf(
        CategorySummary(id = 13L, name = "Investments", type = "INCOME"),
        CategorySummary(id = 2L, name = "Food", type = "EXPENSE")
    )

    @Test
    fun `parses clean complete json with multiple proposals`() {
        val json = """
            {
              "reply": "Ditemukan 2 kupon SBN.",
              "proposals": [
                {
                  "title": "Kupon ST010T4",
                  "amount": 67195.80,
                  "type": "INCOME",
                  "date": 1789120885000,
                  "accountId": 21,
                  "categoryId": 13,
                  "notes": "Direct credit SBN",
                  "tags": ["sbn", "kupon"]
                },
                {
                  "title": "Kupon ST013T2",
                  "amount": 292781.70,
                  "type": "INCOME",
                  "date": 1789120894000,
                  "accountId": 21,
                  "categoryId": 13,
                  "notes": "Direct credit SBN",
                  "tags": ["sbn"]
                }
              ]
            }
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(json, testAccounts, testCategories)
        assertEquals("Ditemukan 2 kupon SBN.", response.reply)
        assertEquals(2, response.proposals.size)
        assertEquals("Kupon ST010T4", response.proposals[0].title)
        assertEquals(6719580L, response.proposals[0].amountInCents)
        assertEquals(21L, response.proposals[0].accountId)
        assertEquals("BNI Taplus", response.proposals[0].accountName)
        assertEquals(13L, response.proposals[0].categoryId)
        assertEquals("Investments", response.proposals[0].categoryName)
        assertEquals("INCOME", response.proposals[0].type)
        assertEquals(listOf("sbn", "kupon"), response.proposals[0].tags)
    }

    @Test
    fun `parses json wrapped in markdown and with think tags`() {
        val raw = """
            <think>
            User has SBN coupons. Let's parse them into JSON.
            </think>
            ```json
            {
              "reply": "Berikut kupon Anda:",
              "proposals": [
                {
                  "title": "Kupon ST012T4",
                  "amount": 442098,
                  "type": "INCOME"
                }
              ]
            }
            ```
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(raw, testAccounts, testCategories)
        assertEquals("Berikut kupon Anda:", response.reply)
        assertEquals(1, response.proposals.size)
        assertEquals("Kupon ST012T4", response.proposals[0].title)
        assertEquals(44209800L, response.proposals[0].amountInCents)
    }

    @Test
    fun `repairs and parses truncated json with complete first proposal`() {
        // Truncated during the second proposal
        val truncated = """
            {
              "reply": "Ditemukan transaksi kupon:",
              "proposals": [
                {
                  "title": "Kupon ST010T4",
                  "amount": 67195.80,
                  "type": "INCOME",
                  "accountId": 21
                },
                {
                  "title": "Kupon ST013T2",
                  "amount": 292781.70,
                  "type": "INCOME"
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(truncated, testAccounts, testCategories)
        assertTrue(response.proposals.isNotEmpty())
        assertEquals("Kupon ST010T4", response.proposals[0].title)
        assertEquals(6719580L, response.proposals[0].amountInCents)
    }

    @Test
    fun `handles severely truncated response at reply text gracefully`() {
        val truncated = """
            {
              "reply": "Halo! Saya telah mendeteksi 3 transaksi penerimaan kupon SBN (Suruat Berjangka Negara)
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(truncated, testAccounts, testCategories)
        assertTrue(response.reply.contains("Halo! Saya telah mendeteksi 3 transaksi"))
        assertEquals(0, response.proposals.size)
    }

    @Test
    fun `handles conversational response without json`() {
        val text = "Strategi 50/30/20 membagi penghasilan menjadi 50% kebutuhan, 30% keinginan, dan 20% tabungan."
        val response = AiJsonParser.parseAssistantResponse(text, testAccounts, testCategories)
        assertEquals(text, response.reply)
        assertTrue(response.proposals.isEmpty())
    }

    @Test
    fun `parses flexible indonesian amounts accurately`() {
        assertEquals(67195.80, AiJsonParser.parseFlexibleAmount("67.195,80"), 0.01)
        assertEquals(67195.80, AiJsonParser.parseFlexibleAmount("67,195.80"), 0.01)
        assertEquals(150000.0, AiJsonParser.parseFlexibleAmount("150.000"), 0.01)
        assertEquals(150000.0, AiJsonParser.parseFlexibleAmount("150,000"), 0.01)
        assertEquals(2500000.0, AiJsonParser.parseFlexibleAmount("Rp 2.500.000"), 0.01)
        assertEquals(442098.0, AiJsonParser.parseFlexibleAmount("442,098.00"), 0.01)
    }

    @Test
    fun `defaults to cash account when no account is specified and investment account is first`() {
        val mixedAccounts = listOf(
            AccountSummary(id = 28L, name = "114538727842", type = "Investment", currency = "IDR"),
            AccountSummary(id = 1L, name = "Wallet", type = "Cash", currency = "IDR"),
            AccountSummary(id = 21L, name = "BNI Taplus", type = "Bank Account", currency = "IDR")
        )

        val json = """
            {
              "reply": "Pengeluaran makan siang terdeteksi",
              "proposals": [
                {
                  "title": "Nasi Padang",
                  "amount": 25000,
                  "type": "EXPENSE"
                }
              ]
            }
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(json, mixedAccounts, testCategories)
        assertEquals(1, response.proposals.size)
        val proposal = response.proposals[0]
        assertEquals(1L, proposal.accountId)
        assertEquals("Wallet", proposal.accountName)
    }

    @Test
    fun `matches cash or tunai accountName to Cash account type`() {
        val mixedAccounts = listOf(
            AccountSummary(id = 28L, name = "114538727842", type = "Investment", currency = "IDR"),
            AccountSummary(id = 5L, name = "Dompet Utama", type = "Cash", currency = "IDR")
        )

        val json = """
            {
              "reply": "Pengeluaran terdeteksi",
              "proposals": [
                {
                  "title": "Kopi Kenangan",
                  "amount": 22000,
                  "type": "EXPENSE",
                  "accountName": "Tunai"
                }
              ]
            }
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(json, mixedAccounts, testCategories)
        assertEquals(1, response.proposals.size)
        assertEquals(5L, response.proposals[0].accountId)
        assertEquals("Dompet Utama", response.proposals[0].accountName)
    }

    @Test
    fun `normalizes 10-digit unix seconds date to milliseconds`() {
        val secondsTimestamp = 1774300000L // 10 digits
        val json = """
            {
              "reply": "Transaksi terdeteksi",
              "proposals": [
                {
                  "title": "Superindo",
                  "amount": 100000,
                  "type": "EXPENSE",
                  "date": $secondsTimestamp
                }
              ]
            }
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(json, testAccounts, testCategories)
        assertEquals(1, response.proposals.size)
        assertEquals(secondsTimestamp * 1000L, response.proposals[0].date)
    }

    @Test
    fun `replaces legacy dummy date 1726000000000 with current timestamp`() {
        val json = """
            {
              "reply": "Transaksi terdeteksi",
              "proposals": [
                {
                  "title": "Superindo",
                  "amount": 100000,
                  "type": "EXPENSE",
                  "date": 1726000000000
                }
              ]
            }
        """.trimIndent()

        val before = System.currentTimeMillis()
        val response = AiJsonParser.parseAssistantResponse(json, testAccounts, testCategories)
        val after = System.currentTimeMillis()

        assertEquals(1, response.proposals.size)
        assertTrue(response.proposals[0].date in before..after)
    }

    @Test
    fun `maps indonesian category names and merchant patterns accurately`() {
        val fullCategories = listOf(
            CategorySummary(id = 1L, name = "Food", type = "EXPENSE"),
            CategorySummary(id = 2L, name = "Utility", type = "EXPENSE"),
            CategorySummary(id = 3L, name = "Shopping", type = "EXPENSE"),
            CategorySummary(id = 4L, name = "Transport", type = "EXPENSE"),
            CategorySummary(id = 5L, name = "Subscriptions", type = "EXPENSE"),
            CategorySummary(id = 10L, name = "Salary", type = "INCOME"),
            CategorySummary(id = 13L, name = "Investments", type = "INCOME")
        )

        val json = """
            {
              "reply": "Deteksi multi transaksi",
              "proposals": [
                {
                  "title": "Beli Nasi Uduk",
                  "amount": 15000,
                  "type": "EXPENSE",
                  "categoryName": "Makanan"
                },
                {
                  "title": "Token PLN Rumah",
                  "amount": 100000,
                  "type": "EXPENSE",
                  "categoryName": "Tagihan Listrik"
                },
                {
                  "title": "Bensin Shell",
                  "amount": 50000,
                  "type": "EXPENSE",
                  "categoryName": "Transportasi"
                },
                {
                  "title": "Netflix Premium",
                  "amount": 186000,
                  "type": "EXPENSE",
                  "categoryName": "Langganan"
                },
                {
                  "title": "Kupon ORI026",
                  "amount": 1500000,
                  "type": "INCOME",
                  "categoryName": "Kupon SBN"
                }
              ]
            }
        """.trimIndent()

        val response = AiJsonParser.parseAssistantResponse(json, testAccounts, fullCategories)
        assertEquals(5, response.proposals.size)
        assertEquals("Food", response.proposals[0].categoryName)
        assertEquals(1L, response.proposals[0].categoryId)

        assertEquals("Utility", response.proposals[1].categoryName)
        assertEquals(2L, response.proposals[1].categoryId)

        assertEquals("Transport", response.proposals[2].categoryName)
        assertEquals(4L, response.proposals[2].categoryId)

        assertEquals("Subscriptions", response.proposals[3].categoryName)
        assertEquals(5L, response.proposals[3].categoryId)

        assertEquals("Investments", response.proposals[4].categoryName)
        assertEquals(13L, response.proposals[4].categoryId)
    }
}
