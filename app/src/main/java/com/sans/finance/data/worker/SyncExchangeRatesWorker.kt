package com.sans.finance.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.ExchangeRateEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@HiltWorker
class SyncExchangeRatesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val currencyDao: CurrencyDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val client = OkHttpClient()
        // Using a free API (v6.exchangerate-api.com has a free tier, but requires key)
        // For demonstration, we'll use a public one that doesn't need key if available,
        // or just fallback to hardcoded rates if it fails.
        val url = "https://open.er-api.com/v6/latest/IDR"

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body.string()
                val json = JSONObject(body)
                val rates = json.getJSONObject("rates")
                val exchangeRates = mutableListOf<ExchangeRateEntity>()

                val keys = rates.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val rateToBase = rates.getDouble(code)
                    // rateToBase is 1 IDR = X CODE
                    // We want rateToIdr (1 CODE = Y IDR)
                    if (rateToBase > 0) {
                        exchangeRates.add(
                            ExchangeRateEntity(
                                code = code,
                                rateToIdr = 1.0 / rateToBase
                            )
                        )
                    }
                }
                currencyDao.insertRates(exchangeRates)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
