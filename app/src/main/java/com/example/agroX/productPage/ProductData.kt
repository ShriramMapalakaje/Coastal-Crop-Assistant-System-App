package com.example.agroX.productPage

import com.example.agroX.presentation.sign_in.UserData
import com.google.firebase.Timestamp

data class Product(
    val id: String = "",              // Firestore document ID
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val quantity: Int = 0,
    val imageUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val user: UserData = UserData()   // Added user details here
)