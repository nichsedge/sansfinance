package com.sans.finance.domain.usecase

import com.sans.finance.domain.repository.CurrencyRepository
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend operator fun invoke(amount: Long, from: String, to: String): Long {
        if (from == to) return amount
        
        // Base currency is IDR in our ExchangeRateEntity
        val fromRate = if (from == "IDR") 1.0 else currencyRepository.getRateToIdr(from) ?: 1.0
        val toRate = if (to == "IDR") 1.0 else currencyRepository.getRateToIdr(to) ?: 1.0
        
        if (toRate == 0.0) return amount
        
        // (amount * fromRate) gives IDR value
        // (IDR value / toRate) gives TO value
        val idrValue = amount * fromRate
        return (idrValue / toRate).toLong()
    }
}
