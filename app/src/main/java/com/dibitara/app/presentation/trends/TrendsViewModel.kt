package com.dibitara.app.presentation.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dibitara.app.domain.model.CategoryTrend
import com.dibitara.app.domain.usecase.GetCategoryTrendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrendsViewModel @Inject constructor(
    getCategoryTrends: GetCategoryTrendsUseCase
) : ViewModel() {

    val trends: StateFlow<List<CategoryTrend>> = getCategoryTrends()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
