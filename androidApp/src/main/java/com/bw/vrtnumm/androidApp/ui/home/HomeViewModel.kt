/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bw.vrtnumm.androidApp.ui.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {
    private val selectedCategory = MutableStateFlow(restore(sharedPreferences = sharedPreferences))
    private val categories = MutableStateFlow(HomeCategory.values().asList())

    private val _state = MutableStateFlow(HomeViewState())

    val state: StateFlow<HomeViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                categories,
                selectedCategory,
            ) { categories, selectedCategory ->
                HomeViewState(
                    homeCategories = categories,
                    selectedHomeCategory = selectedCategory
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }
    }

    fun onHomeCategorySelected(category: HomeCategory) {
        selectedCategory.value = category
        sharedPreferences.edit().
            putString("selectedCategory", category.name)
        .commit()
    }

    companion object {
        private fun restore(sharedPreferences: SharedPreferences): HomeCategory {
            var result = HomeCategory.Discover
                try {
                    result = HomeCategory.valueOf(sharedPreferences.getString("selectedCategory", null)!!)
                } catch (e: Exception) {
                }
            return result
        }
    }
}

enum class HomeCategory {
    Library, Discover, Live, Search
}

data class HomeViewState(
    val selectedHomeCategory: HomeCategory = HomeCategory.Discover,
    val homeCategories: List<HomeCategory> = emptyList()
)
