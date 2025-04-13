package com.example.agroX

import SignInViewModel
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agribot.R
import com.example.agroX.mainScreen.MainPage
import com.example.agroX.presentation.sign_in.GoogleAuthUIClient
import com.example.agroX.presentation.sign_in.SetUsernameScreen
import com.example.agroX.presentation.sign_in.SignInScreen
import com.example.agroX.presentation.sign_in.UserData
import com.example.agroX.ui.theme.AgriBotTheme
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val googleAuthUIClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            FirebaseFirestore.setLoggingEnabled(true) // Debug
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Failed: ${e.message}")
        }

        setContent {
            AgriBotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var userData by remember { mutableStateOf<UserData?>(null) }
                    var isLoading by remember { mutableStateOf(true) }

                    NavHost(
                        navController = navController,
                        startDestination = "loading"
                    ) {
                        composable("loading") {


                            LaunchedEffect(Unit) {
                                checkUserAuth { existingUser ->
                                    userData = existingUser
                                    isLoading = false
                                    when {
                                        existingUser == null -> {
                                            navController.navigate("sign_in") {
                                                popUpTo("loading") { inclusive = true }
                                            }
                                        }
                                        existingUser.userName.isBlank() -> {
                                            navController.navigate("set_username") {
                                                popUpTo("loading") { inclusive = true }
                                            }
                                        }
                                        else -> {
                                            navController.navigate("mainpage") {
                                                popUpTo("loading") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }
                            AgroXLoadingScreen()

//                            LoadingScreen(navController = navController)


                        }

                        composable("sign_in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        lifecycleScope.launch {
                                            val signInResult = googleAuthUIClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            viewModel.onSignInResult(signInResult)
                                            userData = googleAuthUIClient.getSignedInUser()

                                            checkUserAuth { existingUser ->
                                                userData = existingUser
                                                navController.popBackStack()
                                                when {
                                                    existingUser?.userName.isNullOrBlank() -> {
                                                        navController.navigate("set_username")
                                                    }
                                                    else -> {
                                                        navController.navigate("mainpage")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )

                            SignInScreen(
                                state = state,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUIClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                signInIntentSender ?: return@launch
                                            ).build()
                                        )
                                    }
                                }
                            )
                        }

                        composable("set_username") {
                            SetUsernameScreen(
                                navController = navController,
                                onUsernameSet = { newUser ->
                                    // Save to Firestore
                                    val userId = newUser.userId
                                    val username = newUser.userName

                                    if (username.isNotBlank()) {
                                        val firestore = FirebaseFirestore.getInstance()
                                        val batch = firestore.batch()

                                        // 1. Update user document
                                        val userRef = firestore.collection("users").document(userId)
                                        batch.update(userRef, "userName", username)

                                        // 2. Create username document
                                        val usernameRef = firestore.collection("usernames").document(username)
                                        batch.set(usernameRef, mapOf("userId" to userId))

                                        batch.commit()
                                            .addOnSuccessListener {
                                                userData = newUser
                                                navController.navigate("mainpage") {
                                                    popUpTo("set_username") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Failed to save username: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            applicationContext,
                                            "Invalid user data",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                        }

                        composable("mainpage") {
                            MainPage().MainPage(
                                userData = userData,
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUIClient.signOut()
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        userData = null
                                        navController.navigate("sign_in") {
                                            popUpTo("mainpage") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkUserAuth(onResult: (UserData?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            onResult(null)
            return
        }

        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.toObject(UserData::class.java)
                    // Verify username exists in usernames collection
                    if (userData?.userName != null) {
                        FirebaseFirestore.getInstance().collection("usernames")
                            .document(userData.userName)
                            .get()
                            .addOnSuccessListener { usernameDoc ->
                                if (usernameDoc.exists()) {
                                    onResult(userData)
                                } else {
                                    // Username record missing, needs recreation
                                    onResult(userData.copy(userName = ""))
                                }
                            }
                    } else {
                        onResult(userData)
                    }
                } else {
                    onResult(UserData(userId = user.uid, userName = ""))
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}

@Composable
fun LoadingScreen(navController: NavHostController) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    // Add state management
    var loadingComplete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        checkUserAuth(
            onSuccess = { userData ->
                loadingComplete = true
                when {
                    userData?.userName.isNullOrBlank() ->
                        navController.navigate("set_username")
                    else ->
                        navController.navigate("mainpage")
                }
            },
            onError = {
                loadingComplete = true
                navController.navigate("sign_in")
            }
        )
    }

    if (!loadingComplete) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = "Loading",
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Modified checkUserAuth function
private suspend fun checkUserAuth(
    onSuccess: (UserData?) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            // Force token refresh to validate session
            val token = user.getIdToken(true).await()

            // Get user data with timeout
            val userData = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get(Source.SERVER)
                .await()
                .toObject(UserData::class.java)

            // Verify username exists
            if (userData?.userName?.isNotBlank() == true) {
                FirebaseFirestore.getInstance()
                    .collection("usernames")
                    .document(userData.userName)
                    .get()
                    .await()
            }

            onSuccess(userData)
        } ?: run {
            onSuccess(null)
        }
    } catch (e: Exception) {
        FirebaseAuth.getInstance().signOut()
        onError(e)
    }
}

@Composable
fun AgroXLoadingScreen() {
    var showMainText by remember { mutableStateOf(false) }
    var showSubtitle1 by remember { mutableStateOf(false) }
    var showSubtitle2 by remember { mutableStateOf(false) }
    var rotationState by remember { mutableStateOf(0f) }
    var scaleState by remember { mutableStateOf(1f) }

    // Animate scale for final pulse effect
    val scale by animateFloatAsState(
        targetValue = scaleState,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 100f),
        label = "scaleAnimation"
    )

    LaunchedEffect(Unit) {
        // Text entrance sequence
        showMainText = true
        delay(300)
        showSubtitle1 = true
        delay(300)
        showSubtitle2 = true

        // Rotation animation (3 full rotations)
        animate(
            initialValue = 0f,
            targetValue = 1080f,
            animationSpec = tween(1800, easing = FastOutSlowInEasing)
        ) { value, _ ->
            rotationState = value
        }

        // Final pulse effect
        scaleState = 1.2f
        delay(200)
        scaleState = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color.Black else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Main AgroX text
            AnimatedVisibility(
                visible = showMainText,
                enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    AdaptiveImage()
                }
            }

            // Subtitles
//            AnimatedVisibility(
//                visible = showSubtitle1,
//                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
//            ) {
//                Text(
//                    text = "Harvesting Connections,",
//                    style = MaterialTheme.typography.headlineMedium.copy(
//                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
//                        fontSize = 20.sp
//                    )
//                )
//            }
//
//            AnimatedVisibility(
//                visible = showSubtitle2,
//                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
//            ) {
//                Text(
//                    text = "Cultivating Solutions",
//                    style = MaterialTheme.typography.headlineMedium.copy(
//                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
//                        fontSize = 20.sp
//                    )
//                )
//            }

            // Animated icon
//            Spacer(modifier = Modifier.height(48.dp))
//            Icon(
//                imageVector = Icons.Outlined.,
//                contentDescription = null,
//                modifier = Modifier
//                    .size(64.dp)
//                    .rotate(rotationState)
//                    .scale(scale)
//                    .graphicsLayer {
//                        alpha = if (rotationState < 1080f) 1f else 0.9f
//                    },
//                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
//            )
        }


    }
}

@Composable
fun AdaptiveImage() {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes by animateIntAsState(
        targetValue = if (isDarkTheme) {
            R.drawable.agro_dark_logo
        } else {
            R.drawable.agro_light_logo
        },
        animationSpec = tween(durationMillis = 300)
    )

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Application Logo",
        modifier = Modifier
            .size(300.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit
    )
}