package com.example.agroX.mainScreen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import com.example.agroX.presentation.sign_in.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    userData: UserData?,
    onSignOut: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    var showSidebar by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            if (!userData?.profilePictureUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = userData!!.profilePictureUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Name
            if (!userData?.fullName.isNullOrEmpty()) {
                Text(
                    text = userData!!.fullName,
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Username
            if (!userData?.userName.isNullOrEmpty()) {
                Text(
                    text = "@${userData!!.userName}",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bio
            if (!userData?.bio.isNullOrEmpty()) {
                Text(
                    text = userData!!.bio,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
        }

        // Sidebar Trigger Button (Top Right)
        IconButton(
            onClick = { showSidebar = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Sidebar"
            )
        }

        // Custom Right Sidebar
        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally { fullWidth -> fullWidth },
            exit = slideOutHorizontally { fullWidth -> fullWidth },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp)
                    .background(Color.White)
                    .shadow(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Close Button
                    IconButton(
                        onClick = { showSidebar = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Sidebar"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Edit Profile Button
                    Button(
                        onClick = {
                            showSidebar = false
                            navController.navigate("editProfile/${userData?.userId}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Profile")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delete Account Button
                    Button(
                        onClick = {
                            showSidebar = false
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Account")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign Out Button
                    Button(
                        onClick = {
                            showSidebar = false
                            signOutAndClearCache(context)
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            deleteUserAccount(context, navController, onSignOut)
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


// Delete Account Function
fun deleteUserAccount(context: Context, navController: NavController, onSignOut: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    // Delete all user posts
    db.collection("posts")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { posts ->
            val batch = db.batch()
            posts.forEach { batch.delete(it.reference) }
            batch.commit()
        }

    // Delete all user products
    db.collection("products")
        .whereEqualTo("user.userId", userId)
        .get()
        .addOnSuccessListener { products ->
            val batch = db.batch()
            products.forEach { batch.delete(it.reference) }
            batch.commit()
        }

    // Delete user document and authentication record
    db.collection("users").document(userId)
        .delete()
        .addOnCompleteListener {
            user?.delete()?.addOnCompleteListener {
                signOutAndClearCache(context)
                onSignOut()
                navController.navigate("login") {
                    popUpTo(0)
                }
            }
        }
}



// Edit Profile Screen
@Composable
fun EditProfileScreen(
    userId: String,
    navController: NavController,
    onProfileUpdated: (UserData) -> Unit
) {
    var userData by remember { mutableStateOf<UserData?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Load user data from Firestore on first composition.
    LaunchedEffect(userId) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                userData = document.toObject(UserData::class.java)
                loading = false
            }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        userData?.let { data ->
            var fullName by remember { mutableStateOf(data.fullName) }
            var userName by remember { mutableStateOf(data.userName) }
            var bio by remember { mutableStateOf(data.bio) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username Field
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bio Field
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    singleLine = false,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val updatedUser = data.copy(
                            fullName = fullName,
                            userName = userName,
                            bio = bio
                        )
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                onProfileUpdated(updatedUser)
                                navController.popBackStack()
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

/**
 * Signs out the user and clears the cache.
 */
fun signOutAndClearCache(context: Context) {
    val auth = FirebaseAuth.getInstance()
    auth.signOut() // Sign out user

    // Clear Coil Image Cache
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.0).build() } // Clears memory cache
        .diskCache(null) // Disable disk cache
        .build()

    imageLoader.memoryCache?.clear()

    // Clear Shared Preferences
    val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().clear().apply()
}

