package com.example.agroX.mainScreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainPageViewModel : ViewModel() {
    // Store current route as StateFlow to observe changes
    private val _currentRoute = MutableStateFlow("home")
    val currentRoute: StateFlow<String> = _currentRoute

    fun updateRoute(newRoute: String) {
        _currentRoute.value = newRoute
    }
}
