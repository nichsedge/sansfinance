package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.domain.repository.CurrencyRepository
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDao: CurrencyDao
) : CurrencyRepository {
    override suspend fun getRateToIdr(code: String): Double? {
        return currencyDao.getRate(code)?.rateToIdr
    }
}
