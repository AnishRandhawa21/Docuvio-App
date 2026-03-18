package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.docuvio.app.data.repository.OrderRepository

class WalkInOrderViewModelFactory(
    private val orderRepository: OrderRepository,
    private val shopId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(WalkInOrderViewModel::class.java)) {
            return WalkInOrderViewModel(
                orderRepository = orderRepository,
                shopId = shopId
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}