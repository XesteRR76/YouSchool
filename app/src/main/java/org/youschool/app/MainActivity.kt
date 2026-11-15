package org.youschool.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.youschool.app.core.update.AppRelease
import org.youschool.app.core.update.AppUpdateChecker
import org.youschool.app.ui.components.UpdateNotificationBanner
import org.youschool.app.ui.navigation.Screen
import org.youschool.app.ui.screens.auth.AuthScreen
import org.youschool.app.ui.screens.auth.AuthViewModel
import org.youschool.app.ui.screens.grades.GradesScreen
import org.youschool.app.ui.screens.grades.GradesViewModel
import org.youschool.app.ui.screens.homework.HomeworkScreen
import org.youschool.app.ui.screens.homework.HomeworkViewModel
import org.youschool.app.ui.screens.onboarding.OnboardingScreen
import org.youschool.app.ui.screens.profile.ProfileScreen
import org.youschool.app.ui.screens.schedule.ScheduleScreen
import org.youschool.app.ui.screens.schedule.ScheduleViewModel
import org.youschool.app.ui.theme.LocalBottomBarPadding
import org.youschool.app.ui.theme.NavigationBarStyle
import org.youschool.app.ui.theme.YouSchoolTheme
import org.youschool.app.ui.util.rememberHaptic

class MainActivity : ComponentActivity() {
    private var pendingNavigationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        val app = application as YouSchoolApp
        val repository = app.repository

        setContent {
            val themeSettings by app.themeManager.themeSettings.collectAsState()
            val haptic = rememberHaptic()

            YouSchoolTheme(settings = themeSettings) {
                // If logged in and onboarding not completed, show onboarding wizard
                if (repository.isUserLoggedIn() && !themeSettings.hasCompletedOnboarding) {
                    OnboardingScreen(
                        themeManager = app.themeManager,
                        onFinish = {
                            // Onboarding completed, themeSettings will update and transition to main UI
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val startDestination = if (repository.isUserLoggedIn()) Screen.Schedule.route else Screen.Auth.route

                    LaunchedEffect(pendingNavigationRoute) {
                        val route = pendingNavigationRoute
                        if (route != null && repository.isUserLoggedIn()) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            pendingNavigationRoute = null
                        }
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val onTabClick: (String) -> Unit = { route ->
                        haptic.tick()
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    val isFloatingPill = themeSettings.navBarStyle == NavigationBarStyle.FLOATING_PILL && currentRoute != Screen.Auth.route
                    val bottomBarPadding = if (isFloatingPill) (76.dp + themeSettings.navBarBottomOffsetDp.dp) else 0.dp

                    var availableUpdate by remember { mutableStateOf<AppRelease?>(null) }
                    var isUpdateDismissed by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    LaunchedEffect(themeSettings.checkForUpdates) {
                        if (themeSettings.checkForUpdates && !isUpdateDismissed) {
                            val release = AppUpdateChecker.checkForUpdate(context)
                            if (release != null) {
                                availableUpdate = release
                            }
                        } else if (!themeSettings.checkForUpdates) {
                            availableUpdate = null
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (currentRoute != Screen.Auth.route && themeSettings.navBarStyle == NavigationBarStyle.STANDARD) {
                                    NavigationBar {
                                        Screen.bottomNavScreens.forEach { screen ->
                                            NavigationBarItem(
                                                selected = currentRoute == screen.route,
                                                onClick = { onTabClick(screen.route) },
                                                icon = {
                                                    screen.icon?.let {
                                                        Icon(imageVector = it, contentDescription = screen.title)
                                                    }
                                                },
                                                label = { Text(screen.title) }
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            CompositionLocalProvider(LocalBottomBarPadding provides bottomBarPadding) {
                                NavHost(
                                    navController = navController,
                                    startDestination = startDestination,
                                    modifier = Modifier.padding(
                                        top = innerPadding.calculateTopPadding(),
                                        bottom = if (themeSettings.navBarStyle == NavigationBarStyle.STANDARD) innerPadding.calculateBottomPadding() else 0.dp
                                    )
                                ) {
                                    composable(Screen.Auth.route) {
                                        val authViewModel = remember { AuthViewModel(repository) }
                                        AuthScreen(
                                            viewModel = authViewModel,
                                            onLoginSuccess = {
                                                navController.navigate(Screen.Schedule.route) {
                                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                                }
                                            }
                                        )
                                    }

                                    composable(Screen.Schedule.route) {
                                        val scheduleViewModel = remember { ScheduleViewModel(repository) }
                                        ScheduleScreen(viewModel = scheduleViewModel)
                                    }

                                    composable(Screen.Grades.route) {
                                        val gradesViewModel = remember { GradesViewModel(repository) }
                                        GradesScreen(viewModel = gradesViewModel)
                                    }

                                    composable(Screen.Homework.route) {
                                        val homeworkViewModel = remember { HomeworkViewModel(repository) }
                                        HomeworkScreen(viewModel = homeworkViewModel)
                                    }

                                    composable(Screen.Profile.route) {
                                        ProfileScreen(
                                            repository = repository,
                                            themeManager = app.themeManager,
                                            onLogout = {
                                                navController.navigate(Screen.Auth.route) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (isFloatingPill) {
                            FloatingPillNavigationBar(
                                currentRoute = currentRoute,
                                bottomOffsetDp = themeSettings.navBarBottomOffsetDp,
                                onNavigate = onTabClick,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        val bannerBottomPadding = when {
                            currentRoute == Screen.Auth.route -> 16.dp
                            isFloatingPill -> bottomBarPadding + 12.dp
                            themeSettings.navBarStyle == NavigationBarStyle.STANDARD -> 80.dp + 12.dp
                            else -> 16.dp
                        }

                        UpdateNotificationBanner(
                            release = if (!isUpdateDismissed) availableUpdate else null,
                            onDismiss = {
                                isUpdateDismissed = true
                            },
                            onDownloadClick = { rel ->
                                val targetUrl = rel.apkDownloadUrl ?: rel.releaseUrl
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(rel.releaseUrl))
                                        context.startActivity(fallback)
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 16.dp, end = 16.dp, bottom = bannerBottomPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getStringExtra("navigate_to") == "grades") {
            pendingNavigationRoute = Screen.Grades.route
        }
    }
}

@Composable
fun FloatingPillNavigationBar(
    currentRoute: String?,
    bottomOffsetDp: Int = 16,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val screens = Screen.bottomNavScreens
    val tabCount = screens.size
    val density = LocalDensity.current

    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    var hoveredTabIndex by remember { mutableStateOf<Int?>(null) }

    val currentIndex = remember(currentRoute) {
        val idx = screens.indexOfFirst { it.route == currentRoute }
        if (idx != -1) idx else 0
    }
    val effectiveIndex = (hoveredTabIndex ?: currentIndex).coerceIn(0, (tabCount - 1).coerceAtLeast(0))
    val effectiveRoute = screens.getOrNull(effectiveIndex)?.route ?: screens[0].route

    fun getTabIndexAt(x: Float, totalWidth: Float, currentSelectedIdx: Int): Int {
        if (totalWidth <= 0f || tabCount <= 0) return 0
        val ratio = (x / totalWidth).coerceIn(0f, 0.999f)
        if (tabCount == 4) {
            for (i in 0 until 4) {
                val left = if (i <= currentSelectedIdx) i * 0.20f else (i + 1) * 0.20f
                val width = if (i == currentSelectedIdx) 0.40f else 0.20f
                if (ratio >= left && ratio < left + width) {
                    return i
                }
            }
        }
        return (ratio * tabCount).toInt().coerceIn(0, tabCount - 1)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomOffsetDp.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .onSizeChanged { pillWidthPx = it.width.toFloat() }
                .pointerInput(currentRoute, pillWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val initialIndex = getTabIndexAt(down.position.x, pillWidthPx, currentIndex)
                        hoveredTabIndex = initialIndex
                        haptic.tick()

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.find { it.id == pointerId } ?: event.changes.firstOrNull()
                            if (pointer == null || !pointer.pressed) {
                                break
                            }
                            pointer.consume()
                            val newIndex = getTabIndexAt(pointer.position.x, pillWidthPx, hoveredTabIndex ?: currentIndex)
                            if (newIndex != hoveredTabIndex) {
                                hoveredTabIndex = newIndex
                                haptic.tick()
                            }
                        }

                        // Finger released
                        hoveredTabIndex?.let { selectedIdx ->
                            val targetRoute = screens.getOrNull(selectedIdx)?.route
                            if (targetRoute != null && targetRoute != currentRoute) {
                                onNavigate(targetRoute)
                            }
                        }
                        hoveredTabIndex = null
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val totalInnerPaddingPx = with(density) { 12.dp.toPx() }
                val innerWidthPx = (pillWidthPx - totalInnerPaddingPx).coerceAtLeast(0f)
                val targetLeftPx = if (innerWidthPx > 0f) effectiveIndex * (innerWidthPx * 0.20f) else 0f
                val capsuleWidthPx = innerWidthPx * 0.40f

                val animatedLeftPx by animateFloatAsState(
                    targetValue = targetLeftPx,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = 0.82f
                    ),
                    label = "pill_slide"
                )

                // Single fluid sliding background indicator!
                if (innerWidthPx > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedLeftPx.roundToInt(), 0) }
                            .width(with(density) { capsuleWidthPx.toDp() })
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                // Foreground items
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screens.forEachIndexed { index, screen ->
                        val isSelected = effectiveRoute == screen.route
                        val activeContentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "pill_content_$index"
                        )
                        val animatedWeight by animateFloatAsState(
                            targetValue = if (isSelected) 1.6f else 0.8f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = 0.82f
                            ),
                            label = "tab_weight_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(animatedWeight)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                screen.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        tint = activeContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(animationSpec = tween(200)),
                                    exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(animationSpec = tween(150))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = activeContentColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
