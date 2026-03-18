package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.data.model.Shop
import com.docuvio.app.data.repository.Result
import com.docuvio.app.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val shops: List<Shop> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadShops()
    }

    fun loadShops() {

        // prevent duplicate calls
        if (_uiState.value.shops.isNotEmpty()) return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = shopRepository.getShops()) {

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        shops = result.data,
                        isLoading = false
                    )
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshShops() {

        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isRefreshing = true)

            when (val result = shopRepository.getShops()) {

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        shops = result.data,
                        isRefreshing = false
                    )
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isRefreshing = false
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false
                    )
                }
            }
        }
    }
}
