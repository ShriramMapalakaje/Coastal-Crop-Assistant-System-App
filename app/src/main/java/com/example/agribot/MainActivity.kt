package com.example.agribot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agribot.MainScreen.MainPage
import com.example.agribot.presentation.sign_in.GoogleAuthUIClient
import com.example.agribot.presentation.sign_in.SignInScreen
import com.example.agribot.presentation.sign_in.SignInViewModel
import com.example.agribot.ui.theme.AgriBotTheme
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUIClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgriBotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 🔥 Store userData in remember to trigger recomposition when changed
                    var userData by remember { mutableStateOf(googleAuthUIClient.getSignedInUser()) }

                    NavHost(navController = navController, startDestination = if (userData != null) "mainpage" else "sign_in") {
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
                                            userData = googleAuthUIClient.getSignedInUser()  // 🔥 Update user state
                                        }
                                    }
                                }
                            )

                            LaunchedEffect(state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(applicationContext, "Sign in successful", Toast.LENGTH_LONG).show()
                                    userData = googleAuthUIClient.getSignedInUser()  // 🔥 Ensure user state updates
                                    navController.navigate("mainpage") {
                                        popUpTo("sign_in") { inclusive = true }  // 🔥 Prevent back navigation to sign-in
                                    }
                                    viewModel.resetState()
                                }
                            }

                            SignInScreen(
                                state = state,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUIClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(signInIntentSender ?: return@launch).build()
                                        )
                                    }
                                }
                            )
                        }

                        composable("mainpage") {
                            val mainPage = MainPage()
                            mainPage.MainPage(
                                userData = userData,
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUIClient.signOut()
                                        Toast.makeText(applicationContext, "Signed out", Toast.LENGTH_LONG).show()
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
}



