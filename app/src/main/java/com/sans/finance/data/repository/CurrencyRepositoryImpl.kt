package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.FxRateEntity
import com.sans.finance.domain.model.FxRate
import com.sans.finance.domain.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDao: CurrencyDao,
    private val httpClient: OkHttpClient
) : CurrencyRepository {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    override suspend fun getRateToIdr(code: String): Double? {
        return currencyDao.getRate(code.uppercase().trim())?.rateToIdr
    }

    override suspend fun getHistoricalRate(
        fromCurrency: String,
        toCurrency: String,
        dateMillis: Long
    ): Double? {
        val dateStr = synchronized(isoDateFormat) {
            isoDateFormat.format(Date(dateMillis))
        }
        return getHistoricalRate(fromCurrency, toCurrency, dateStr)
    }

    override suspend fun getHistoricalRate(
        fromCurrency: String,
        toCurrency: String,
        date: String
    ): Double? = withContext(Dispatchers.IO) {
        val from = fromCurrency.uppercase().trim()
        val to = toCurrency.uppercase().trim()

        if (from == to) return@withContext 1.0

        // 1. Try local room database lookup
        val localRate = findLocalFxRate(from, to, date)
        if (localRate != null) return@withContext localRate

        // 2. Fallback to online fetch for that date
        val fetchedRate = fetchAndCacheHistoricalRates(from, to, date)
        if (fetchedRate != null) return@withContext fetchedRate

        // 3. Fallback to nearest previous available date in DB (for weekends/holidays)
        val nearestRate = findNearestLocalFxRate(from, to, date)
        if (nearestRate != null) return@withContext nearestRate

        // 4. Fallback to latest exchange rates table
        fallbackToLatestExchangeRate(from, to)
    }

    private suspend fun findLocalFxRate(from: String, to: String, date: String): Double? {
        val direct = currencyDao.getFxRate("$from/$to", date)?.rate
        if (direct != null && direct > 0.0) return direct

        val inverse = currencyDao.getFxRate("$to/$from", date)?.rate
        if (inverse != null && inverse > 0.0) return 1.0 / inverse

        // Triangulate via IDR
        val fromToIdr = if (from == "IDR") 1.0 else currencyDao.getFxRate("$from/IDR", date)?.rate
            ?: (currencyDao.getFxRate("IDR/$from", date)?.rate?.let { if (it > 0) 1.0 / it else null })
        val toToIdr = if (to == "IDR") 1.0 else currencyDao.getFxRate("$to/IDR", date)?.rate
            ?: (currencyDao.getFxRate("IDR/$to", date)?.rate?.let { if (it > 0) 1.0 / it else null })

        if (fromToIdr != null && toToIdr != null && toToIdr > 0.0) {
            return fromToIdr / toToIdr
        }

        // Triangulate via USD
        val fromToUsd = if (from == "USD") 1.0 else currencyDao.getFxRate("$from/USD", date)?.rate
            ?: (currencyDao.getFxRate("USD/$from", date)?.rate?.let { if (it > 0) 1.0 / it else null })
        val toToUsd = if (to == "USD") 1.0 else currencyDao.getFxRate("$to/USD", date)?.rate
            ?: (currencyDao.getFxRate("USD/$to", date)?.rate?.let { if (it > 0) 1.0 / it else null })

        if (fromToUsd != null && toToUsd != null && toToUsd > 0.0) {
            return fromToUsd / toToUsd
        }

        return null
    }

    private suspend fun findNearestLocalFxRate(from: String, to: String, date: String): Double? {
        val direct = currencyDao.getLatestFxRateOnOrBefore("$from/$to", date)?.rate
        if (direct != null && direct > 0.0) return direct

        val inverse = currencyDao.getLatestFxRateOnOrBefore("$to/$from", date)?.rate
        if (inverse != null && inverse > 0.0) return 1.0 / inverse

        val fromToIdr = if (from == "IDR") 1.0 else currencyDao.getLatestFxRateOnOrBefore("$from/IDR", date)?.rate
        val toToIdr = if (to == "IDR") 1.0 else currencyDao.getLatestFxRateOnOrBefore("$to/IDR", date)?.rate

        if (fromToIdr != null && toToIdr != null && toToIdr > 0.0) {
            return fromToIdr / toToIdr
        }

        return null
    }

    private suspend fun fetchAndCacheHistoricalRates(from: String, to: String, date: String): Double? {
        // Use frankfurter API (supports historical daily rates back to 1999)
        val urls = listOf(
            "https://api.frankfurter.app/$date?from=USD",
            "https://api.frankfurter.dev/v1/$date?from=USD"
        )

        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val json = JSONObject(body)
                    val actualDate = json.optString("date", date)
                    val ratesObj = json.getJSONObject("rates")

                    val entities = mutableListOf<FxRateEntity>()
                    val usdToIdr = if (ratesObj.has("IDR")) ratesObj.getDouble("IDR") else null

                    // Base is USD
                    entities.add(FxRateEntity(currencyPair = "USD/USD", date = actualDate, rate = 1.0))
                    if (date != actualDate) {
                        entities.add(FxRateEntity(currencyPair = "USD/USD", date = date, rate = 1.0))
                    }

                    val keys = ratesObj.keys()
                    while (keys.hasNext()) {
                        val sym = keys.next()
                        val rate = ratesObj.getDouble(sym)
                        if (rate > 0) {
                            // USD -> SYM
                            entities.add(FxRateEntity(currencyPair = "USD/$sym", date = actualDate, rate = rate))
                            entities.add(FxRateEntity(currencyPair = "$sym/USD", date = actualDate, rate = 1.0 / rate))

                            if (date != actualDate) {
                                entities.add(FxRateEntity(currencyPair = "USD/$sym", date = date, rate = rate))
                                entities.add(FxRateEntity(currencyPair = "$sym/USD", date = date, rate = 1.0 / rate))
                            }

                            // SYM -> IDR cross rate
                            if (usdToIdr != null && usdToIdr > 0) {
                                val symToIdr = (1.0 / rate) * usdToIdr
                                entities.add(FxRateEntity(currencyPair = "$sym/IDR", date = actualDate, rate = symToIdr))
                                entities.add(FxRateEntity(currencyPair = "IDR/$sym", date = actualDate, rate = 1.0 / symToIdr))

                                if (date != actualDate) {
                                    entities.add(FxRateEntity(currencyPair = "$sym/IDR", date = date, rate = symToIdr))
                                    entities.add(FxRateEntity(currencyPair = "IDR/$sym", date = date, rate = 1.0 / symToIdr))
                                }
                            }
                        }
                    }

                    if (usdToIdr != null && usdToIdr > 0) {
                        entities.add(FxRateEntity(currencyPair = "USD/IDR", date = actualDate, rate = usdToIdr))
                        entities.add(FxRateEntity(currencyPair = "IDR/USD", date = actualDate, rate = 1.0 / usdToIdr))
                        if (date != actualDate) {
                            entities.add(FxRateEntity(currencyPair = "USD/IDR", date = date, rate = usdToIdr))
                            entities.add(FxRateEntity(currencyPair = "IDR/USD", date = date, rate = 1.0 / usdToIdr))
                        }
                    }

                    currencyDao.insertFxRates(entities)

                    // Find rate for requested pair
                    return findLocalFxRate(from, to, date) ?: findLocalFxRate(from, to, actualDate)
                }
            } catch (e: Exception) {
                // Continue to next provider or fallback
            }
        }

        // Secondary fallback to exchangerate.host or open.er-api.com
        try {
            val fallbackUrl = "https://open.er-api.com/v6/latest/USD"
            val request = Request.Builder().url(fallbackUrl).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body.string()
                val json = JSONObject(body)
                val ratesObj = json.getJSONObject("rates")
                val entities = mutableListOf<FxRateEntity>()
                val usdToIdr = if (ratesObj.has("IDR")) ratesObj.getDouble("IDR") else null

                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val sym = keys.next()
                    val rate = ratesObj.getDouble(sym)
                    if (rate > 0) {
                        entities.add(FxRateEntity(currencyPair = "USD/$sym", date = date, rate = rate))
                        entities.add(FxRateEntity(currencyPair = "$sym/USD", date = date, rate = 1.0 / rate))
                        if (usdToIdr != null && usdToIdr > 0) {
                            val symToIdr = (1.0 / rate) * usdToIdr
                            entities.add(FxRateEntity(currencyPair = "$sym/IDR", date = date, rate = symToIdr))
                            entities.add(FxRateEntity(currencyPair = "IDR/$sym", date = date, rate = 1.0 / symToIdr))
                        }
                    }
                }
                currencyDao.insertFxRates(entities)
                return findLocalFxRate(from, to, date)
            }
        } catch (e: Exception) {
            // Ignored
        }

        return null
    }

    private suspend fun fallbackToLatestExchangeRate(from: String, to: String): Double? {
        val fromRate = if (from == "IDR") 1.0 else currencyDao.getRate(from)?.rateToIdr
        val toRate = if (to == "IDR") 1.0 else currencyDao.getRate(to)?.rateToIdr

        if (fromRate != null && toRate != null && toRate > 0.0) {
            return fromRate / toRate
        }
        return null
    }

    override suspend fun backfillHistoricalRates(
        dates: List<String>,
        baseCurrencies: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val distinctDates = dates.distinct()
        for (date in distinctDates) {
            val existing = currencyDao.getFxRatesForDate(date)
            if (existing.isEmpty()) {
                fetchAndCacheHistoricalRates("USD", "IDR", date)
            }
        }
    }

    override fun getFxRatesForPair(currencyPair: String): Flow<List<FxRate>> {
        return currencyDao.getFxRatesForPair(currencyPair).map { list ->
            list.map { FxRate(it.currencyPair, it.date, it.rate, it.createdAt) }
        }
    }

    override suspend fun saveFxRate(currencyPair: String, date: String, rate: Double) {
        currencyDao.insertFxRate(
            FxRateEntity(
                currencyPair = currencyPair,
                date = date,
                rate = rate
            )
        )
    }
}
