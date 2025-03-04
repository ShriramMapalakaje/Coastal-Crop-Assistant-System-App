package com.example.agribot.MainScreen



import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.agribot.presentation.sign_in.UserData


data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

class MainPage : ViewModel() {
    @Composable
    fun MainPage(
        userData: UserData?,
        onSignOut: () -> Unit
    ){
        MainPageScreen(userData , onSignOut)
    }
}

@Composable
fun MainPageScreen(
    userData: UserData?,
    onSignOut: () -> Unit,
    viewModel: MainPageViewModel = androidx.lifecycle.viewmodel.compose.viewModel() // ViewModel instance
) {
    val navController = rememberNavController()
    val currentRoute by viewModel.currentRoute.collectAsState()

    // Reset navigation on user change
    LaunchedEffect(userData) {
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController, viewModel) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("profile") { ProfileScreen(userData, onSignOut) }
            composable("chatbot") { ChatbotScreen() }
        }
    }

    // Observe navigation changes and update ViewModel
    LaunchedEffect(currentRoute) {
        navController.navigate(currentRoute) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
        }
    }
}





@Composable
fun BottomNavigationBar(navController: NavHostController, viewModel: MainPageViewModel) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val items = listOf(
        NavigationItem("home", "Home", Icons.Default.Home),
        NavigationItem("chatbot", "Chat Bot", Icons.Default.Face),
        NavigationItem("profile", "Profile", Icons.Default.Person)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                label = { Text(item.label) },
                selected = currentBackStackEntry?.destination?.route == item.route,
                onClick = {
                    viewModel.updateRoute(item.route) // Update ViewModel when clicked
                },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) }
            )
        }
    }
}
