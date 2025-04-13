import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroX.presentation.sign_in.SignInResult
import com.example.agroX.presentation.sign_in.SignInState
import com.example.agroX.presentation.sign_in.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    init {
        checkSignedInUser() // ✅ Check user authentication on launch
    }

    fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSignInSuccessful = result.data != null,
                    userData = result,
                    signInError = result.errorMessage,
                    isLoading = false
                )
            }
        }
    }

    private fun checkSignedInUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val fullName = currentUser.displayName ?: "Unknown"
            val profilePictureUrl = currentUser.photoUrl?.toString()

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val username = document.getString("userName") ?: ""

                    _state.update {
                        it.copy(
                            isSignInSuccessful = true,
                            userData = SignInResult(
                                data = UserData(
                                    userId = userId,
                                    fullName = fullName,
                                    userName = username,
                                    profilePictureUrl = profilePictureUrl
                                ),
                                errorMessage = null
                            )
                        )
                    }
                }
                .addOnFailureListener { e ->
                    _state.update {
                        it.copy(signInError = "Failed to retrieve user: ${e.message}")
                    }
                }
        }
    }

    fun resetState() {
        _state.update { SignInState() }
    }
}
