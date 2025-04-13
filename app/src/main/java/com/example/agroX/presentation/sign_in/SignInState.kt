package com.example.agroX.presentation.sign_in

data class SignInState(
    val isSignInSuccessful: Boolean = false,
    val isLoading: Boolean = false,
    val userData: SignInResult? = null,  // ✅ Now it's nullable
    val signInError: String? = null
)
