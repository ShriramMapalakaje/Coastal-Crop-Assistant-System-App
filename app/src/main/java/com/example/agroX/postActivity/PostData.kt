package com.example.agroX.postActivity
data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val profilePictureUrl: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val imageWidth: Int = 0,  // ✅ Store image width
    val imageHeight: Int = 0, // ✅ Store image height
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val likedUsers: List<String> = emptyList()
)
