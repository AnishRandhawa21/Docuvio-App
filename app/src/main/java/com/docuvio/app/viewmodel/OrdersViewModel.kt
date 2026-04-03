package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.data.model.Order
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job


data class OrdersUiState(
    val isLoading: Boolean = false,
    val currentOrders: List<Order> = emptyList(),
    val orderHistory: List<Order> = emptyList(),
    val error: String? = null
)

class OrdersViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    /** Prevents duplicate API calls */
    private var loadJob: Job? = null

    init {
        loadOrders()
    }

    /**
     * Load orders for current logged-in user
     */
    fun loadOrders(force: Boolean = false) {

        if (!force && _uiState.value.currentOrders.isNotEmpty()) return

        loadJob?.cancel()

        loadJob = viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = orderRepository.getOrders()) {

                is Result.Success -> {

                    val allOrders = result.data.data

                    val currentOrders = allOrders.filter {
                        !it.isExpired &&
                                !it.status.equals("completed", true) &&
                                !it.status.equals("cancelled", true)
                    }

                    val historyOrders = allOrders.filter {
                        it.isExpired ||
                                it.status.equals("completed", true) ||
                                it.status.equals("cancelled", true)
                    }

                    _uiState.value = OrdersUiState(
                        currentOrders = currentOrders,
                        orderHistory = historyOrders,
                        isLoading = false
                    )
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }

                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

}