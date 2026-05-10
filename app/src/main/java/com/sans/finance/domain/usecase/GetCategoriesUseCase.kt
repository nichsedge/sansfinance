package com.sans.finance.domain.usecase

import com.sans.finance.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val dao: com.sans.finance.data.local.dao.CategoryDao
) {
    operator fun invoke(): Flow<List<CategoryEntity>> {
        return dao.getAllCategories()
    }
}
