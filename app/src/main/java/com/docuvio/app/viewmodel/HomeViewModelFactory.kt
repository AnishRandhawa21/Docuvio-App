package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.docuvio.app.data.repository.ShopRepository

class HomeViewModelFactory(
    private val shopRepository: ShopRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(shopRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}