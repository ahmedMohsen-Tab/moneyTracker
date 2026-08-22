package com.moneytracker.data.repository

import com.moneytracker.data.local.dao.CategoryDao
import com.moneytracker.data.mapper.toCategory
import com.moneytracker.data.mapper.toEntity
import com.moneytracker.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toCategory() } }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category.toEntity())
    }

    suspend fun getMaxCategoryId(): Int = categoryDao.getMaxId()
}
