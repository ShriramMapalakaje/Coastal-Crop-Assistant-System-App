package com.example.agroX.mainScreen



import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.agroX.presentation.sign_in.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

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
    viewModel: MainPageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val navController = rememberNavController()
    val currentRoute by viewModel.currentRoute.collectAsState()

    var showUploadSheet by remember { mutableStateOf(false) }
    val isConnected = rememberConnectivityState()

    var showStatus by remember { mutableStateOf(false) }
    var wasConnectedBefore by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // ✅ Handle status visibility
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            showStatus = true
            wasConnectedBefore = false
            coroutineScope.launch {
                delay(3000) // Auto-hide after 3 seconds
                showStatus = false
            }
        } else if (!wasConnectedBefore) {
            showStatus = true
            coroutineScope.launch {
                delay(3000) // Auto-hide after 3 seconds
                showStatus = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ✅ Connection Status Bar at the Top


        // ✅ Main Content (Nav Host + Bottom Bar)
        Scaffold(
            bottomBar = { BottomNavigationBar(navController, viewModel) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController, onUploadClick = { showUploadSheet = true }) }
                    composable("profile") { ProfileScreen(userData, onSignOut, navController) }
                    composable("chatbot") { ChatbotScreen(navController) }
                    composable("products") { ProductRecommendation(navController) }
                    composable("editProfile/{userId}") { backStackEntry ->
                        EditProfileScreen(
                            userId = backStackEntry.arguments?.getString("userId") ?: "",
                            navController = navController,
                            onProfileUpdated = { updatedUser ->
                                // Update your view model or state holder here
                            }
                        )
                    }
                }

                if (showUploadSheet) {
                    UploadBottomSheet(navController = navController, onDismiss = { showUploadSheet = false })
                }
            }
        }
    }
    AnimatedVisibility(
        visible = showStatus,
        enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isConnected) Color(0xB072DF72)
                    else MaterialTheme.colorScheme.errorContainer
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isConnected) "Online" else "No Internet Connection",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp
            )
        }
    }


    LaunchedEffect(currentRoute) {
        navController.navigate(currentRoute) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    viewModel: MainPageViewModel
) {
    val items = listOf(
        NavigationItem("home", "Home", Icons.Outlined.Home, badgeCount = 0),
        NavigationItem("products", "Products", Icons.Outlined.ShoppingCart, badgeCount = 0),
        NavigationItem("chatbot", "Chat", Icons.AutoMirrored.Outlined.Chat),
        NavigationItem("profile", "Profile", Icons.Outlined.Person, badgeCount = 0)
    )
    CustomBottomNavigationBar(navController, viewModel, items)
}

@Composable
fun CustomBottomNavigationBar(
    navController: NavHostController,
    viewModel: MainPageViewModel,
    items: List<NavigationItem>
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    // State to track the width of the navigation bar
    var barWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val itemCount = items.size
    val itemWidthPx = if (barWidth > 0) barWidth / itemCount else 0f
    val itemWidthDp = with(density) { itemWidthPx.toDp() }

    // Indicator properties
    val indicatorSize = 8.dp
    val targetOffsetX = if (barWidth > 0) {
        selectedIndex * itemWidthDp + (itemWidthDp / 2 - indicatorSize / 2)
    } else {
        0.dp
    }
    val animatedOffsetX by animateDpAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Items Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { barWidth = it.width.toFloat() },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!isSelected) {
                                viewModel.updateRoute(item.route)
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        if (item.badgeCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp),
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(
                                    text = item.badgeCount.coerceAtMost(99).toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        // Animated Indicator
        if (barWidth > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = animatedOffsetX, y = 0.dp)
                    .size(indicatorSize)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
        }
    }
}

@Composable
fun rememberConnectivityState(): Boolean {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(checkInternetConnection(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            val newState = checkInternetConnection(context)
            if (newState != isConnected) {
                isConnected = newState
            }
            delay(3000) // Check every 3 seconds
        }
    }

    return isConnected
}

fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}