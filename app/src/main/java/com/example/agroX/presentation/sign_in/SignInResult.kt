package com.example.agroX.presentation.sign_in



data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)

data class UserData(
    val userId: String = "",
    val userName: String = "",
    val fullName: String = "User",
    val email: String? = null,
    val phoneNumber: String? = null,
    val profilePictureUrl: String? = null,
    val bio: String = "",
    val authProviders: List<String> = emptyList(),
)
