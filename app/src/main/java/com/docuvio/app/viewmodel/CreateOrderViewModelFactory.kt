package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.ShopRepository

class CreateOrderViewModelFactory(
    private val shopRepository: ShopRepository,
    private val orderRepository: OrderRepository,
    private val shopId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateOrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateOrderViewModel(
                shopRepository = shopRepository,
                orderRepository = orderRepository,
                shopId = shopId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
