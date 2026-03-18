package com.docuvio.app.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.docuvio.app.ui.navigation.Routes
import com.docuvio.app.theme.AlmostBlack
import com.docuvio.app.theme.Cream
import com.docuvio.app.theme.SoftPink

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {

    val bottomNavItems = listOf(
        BottomNavItem(Routes.Home.route, Icons.Default.Home, "Home"),
        BottomNavItem(Routes.Orders.route, Icons.AutoMirrored.Filled.List, "Orders"),
        BottomNavItem(Routes.Profile.route, Icons.Default.Person, "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedIndex =
        bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    var containerWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val circleOffset by animateDpAsState(
        targetValue = with(density) {
            when (selectedIndex) {
                0 -> 40.dp
                1 -> (containerWidth / 2f).toDp() - 26.dp
                2 -> containerWidth.toDp() - 92.dp
                else -> (containerWidth / 2f).toDp() - 26.dp
            }
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "circleOffset"
    )

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = Cream,
        contentColor = AlmostBlack,

        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .onGloballyPositioned {
                                containerWidth = it.size.width
                            },
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(35.dp),
                        color = AlmostBlack,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {

                            if (containerWidth > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .offset(x = circleOffset, y = 9.dp)
                                        .background(
                                            color = SoftPink,
                                            shape = CircleShape
                                        )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 40.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentRoute == item.route

                                    IconButton(
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(
                                                        navController.graph
                                                            .findStartDestination().id
                                                    ) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (selected) AlmostBlack else Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            content(Modifier.fillMaxSize())

            // ONLY bottom fade (small + soft)
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(36.dp)
//                    .align(Alignment.BottomCenter)
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(
//                                Color.Transparent,
//                                Black.copy(alpha = 0.35f),
//                                Black.copy(alpha = 0.75f)
//                            )
//                        )
//                    )
//            )
        }
    }
}