package com.example.agroX.presentation.sign_in

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.agribot.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class GoogleAuthUIClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun signIn(): IntentSender? {
        return try {
            oneTapClient.beginSignIn(buildSignInRequest()).await().pendingIntent.intentSender
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            null
        }
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

        return try {
            val authResult = auth.signInWithCredential(googleCredential).await()
            val user = authResult.user ?: return SignInResult(null, "Failed to retrieve user")

            val userId = user.uid
            val email = user.email ?: ""  // ✅ Retrieve email

            // ✅ Fetch user data in a single Firestore call
            val document = firestore.collection("users").document(userId).get().await()
            val existingUser = document.toObject(UserData::class.java)

            val userData = existingUser?.userName?.let {
                UserData(
                    userId = userId,
                    fullName = user.displayName ?: "Unknown",
                    userName = it,
                    email = email,  // ✅ Store email
                    profilePictureUrl = user.photoUrl?.toString(),
                    bio = existingUser?.bio ?: ""
                )
            }

            // ✅ If user does not exist, store it immediately
            if (existingUser == null) {
                val newUserData = UserData(
                    userId = userId,
                    fullName = user.displayName ?: "Unknown",
                    userName = "user_${userId.takeLast(6)}", // Generate default username
                    email = email,  // ✅ Save email in Firestore
                    profilePictureUrl = user.photoUrl?.toString()
                )

                firestore.collection("users").document(userId).set(newUserData).await()
            }

            SignInResult(data = userData, errorMessage = null)
        } catch (e: Exception) {
            e.printStackTrace()
            SignInResult(data = null, errorMessage = e.message)
        }
    }


    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

    suspend fun getSignedInUser(): UserData? {
        val user = auth.currentUser ?: return null

        return try {
            // ✅ Use Firestore listener instead of fetching again
            val document = firestore.collection("users").document(user.uid).get().await()
            document.toObject(UserData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
