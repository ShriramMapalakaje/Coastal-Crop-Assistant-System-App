package com.example.agroX.presentation.sign_in

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun SetUsernameScreen(
    navController: NavController,
    onUsernameSet: (UserData) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var rawUsername by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) } // ✅ Now properly tracks availability
    val coroutineScope = rememberCoroutineScope()

    val normalizedUsername = rawUsername.trim().lowercase()

    fun checkUsernameAvailability(username: String) {
        val trimmedUsername = username.trim().lowercase()

        if (trimmedUsername.length < 3) {
            isUsernameAvailable = null
            errorMessage = "Username must be at least 3 characters."
            return
        }

        isCheckingUsername = true
        val firestore = FirebaseFirestore.getInstance()
        val usernamesRef = firestore.collection("usernames") // Ensure usernames are stored as documents

        usernamesRef.document(trimmedUsername).get()
            .addOnSuccessListener { document ->
                isCheckingUsername = false
                if (document.exists()) {
                    isUsernameAvailable = false
                    errorMessage = "Username already taken. Try another one."
                } else {
                    isUsernameAvailable = true
                    errorMessage = ""
                }
            }
            .addOnFailureListener {
                isCheckingUsername = false
                isUsernameAvailable = null
                errorMessage = "Error checking username. Try again."
            }
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Complete Registration",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 🔹 Username Input Field
            OutlinedTextField(
                value = rawUsername,
                onValueChange = { newValue ->
                    rawUsername = newValue.filter { it.isLetterOrDigit() || it == '_' }
                    errorMessage = ""
                    isUsernameAvailable = null // Reset availability check

                    coroutineScope.launch {
                        delay(500) // Debounce Firestore calls to avoid excessive reads
                        checkUsernameAvailability(rawUsername)
                    }
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text
                ),
                trailingIcon = {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else if (isUsernameAvailable == true) {
                        Icon(Icons.Default.Check, contentDescription = "Available", tint = Color.Green)
                    } else if (isUsernameAvailable == false) {
                        Icon(Icons.Default.Close, contentDescription = "Taken", tint = Color.Red)
                    }
                }
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            // 🔹 Show Phone Number & OTP Only If Username is Available
            if (isUsernameAvailable == true) { // ✅ Only show if username is unique
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it.filter { c -> c.isDigit() || c == '+' }
                        errorMessage = ""
                    },
                    label = { Text("Phone Number (+CountryCode)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                if (isOtpSent) {
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it.filter { c -> c.isDigit() } },
                        label = { Text("Enter OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (!isOtpSent) {
                            sendOtp(
                                activity, normalizedUsername, phoneNumber, navController, onUsernameSet,
                                onSuccess = { id ->
                                    verificationId = id
                                    isOtpSent = true
                                    errorMessage = ""
                                },
                                onError = { message -> errorMessage = message }
                            )
                        } else {
                            verifyOtp(
                                activity, verificationId, otp, normalizedUsername, phoneNumber, navController,
                                onSuccess = { user ->
                                    createUserRecord(
                                        user, normalizedUsername, phoneNumber, context, navController, onUsernameSet
                                    )
                                },
                                onError = { message ->
                                    errorMessage = message
                                    isOtpSent = false
                                }
                            )
                        }
                    },
                    enabled = when {
                        !isOtpSent -> isUsernameAvailable == true && isValidPhoneNumber(phoneNumber) && !isLoading
                        else -> otp.length == 6 && !isLoading
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isOtpSent) "Verify OTP" else "Send OTP",
                            fontSize = 16.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}



private fun sendOtp(
    activity: Activity,
    username: String,
    phoneNumber: String,
    navController: NavController,
    onUsernameSet: (UserData) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    when {
        username.length < 3 -> {
            onError("Username must be at least 3 characters")
            return
        }
        !isValidPhoneNumber(phoneNumber) -> {
            onError("Invalid phone number format. Use +CountryCodeXXXXXXXXXX")
            return
        }
    }

    val db = FirebaseFirestore.getInstance()
    db.collection("users")
        .whereEqualTo("phoneNumber", phoneNumber)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val existingUser = documents.documents.first()
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (existingUser.id != currentUser?.uid) {
                    onError("This phone number is already linked to another account. Try signing in instead.")
                    return@addOnSuccessListener
                }
            }

            // Proceed to send OTP
            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        signInWithPhoneCredential(
                            credential = credential,
                            username = username,
                            phoneNumber = phoneNumber,
                            navController = navController,
                            context = activity,
                            onSuccess = onUsernameSet,
                            onError = onError
                        )
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        onError("Verification failed: ${e.message}")
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        onSuccess(verificationId)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
        .addOnFailureListener { e ->
            onError("Failed to check phone number: ${e.message}")
        }
}



private fun signInWithPhoneCredential(
    credential: PhoneAuthCredential,
    username: String,  // ✅ Pass username
    phoneNumber: String,  // ✅ Pass phone number
    navController: NavController,
    context: Context,
    onSuccess: (UserData) -> Unit,
    onError: (String) -> Unit
) {
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = task.result?.user
                if (user != null) {
                    // ✅ Ensure phone number is stored
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection("users").document(user.uid)

                    userRef.update("phoneNumber", phoneNumber)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Phone number updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update phone number: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    createUserRecord(user, username, phoneNumber, context, navController, onSuccess)
                } else {
                    onError("Authentication failed")
                }
            } else {
                onError("Verification failed: ${task.exception?.message}")
            }
        }
}
private fun createUserRecord(
    user: FirebaseUser,
    username: String,
    phoneNumber: String,
    context: Context,
    navController: NavController,
    onSuccess: (UserData) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(user.uid)

    userRef.get().addOnSuccessListener { document ->
        val fullName = user.displayName ?: "User"
        val email = user.email ?: ""

        val userData = hashMapOf(
            "userId" to user.uid,
            "userName" to username,  // ✅ Ensure userName updates correctly
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,  // ✅ Ensure phoneNumber updates
            "profilePictureUrl" to (user.photoUrl?.toString() ?: "")
        )

        userRef.set(userData, SetOptions.merge())  // 🔥 Ensures updates are applied
            .addOnSuccessListener {
                onSuccess(
                    UserData(user.uid, username, fullName, email, phoneNumber, user.photoUrl?.toString())
                )
                navController.navigate("mainpage") { popUpTo("set_username") }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
// 2. Modified verification flow
private fun verifyOtp(
    activity: Activity,
    verificationId: String?,
    otp: String,
    username: String,
    phoneNumber: String,
    navController: NavController,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    if (verificationId == null || otp.length != 6) {
        onError("Invalid OTP. Please re-enter your phone number.")
        return
    }

    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser != null) {
        currentUser.linkWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(currentUser)
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // 🔹 Phone number already linked, show error
                        onError("This phone number is already linked to another account. Try signing in instead.")
                    } else {
                        onError("Linking failed: ${exception?.message}")
                    }
                }
            }
    } else {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    onSuccess(signInTask.result?.user!!)
                } else {
                    val exception = signInTask.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        onError("This phone number is already linked to another account. Try signing in instead.")
                    } else {
                        onError("Invalid OTP. Please re-enter your phone number.")
                    }
                }
            }
    }
}


private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.matches(Regex("^\\+[1-9]\\d{1,14}\$"))
}

