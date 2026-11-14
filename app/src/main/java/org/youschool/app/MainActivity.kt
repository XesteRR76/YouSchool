package org.youschool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.youschool.app.ui.navigation.Screen
import org.youschool.app.ui.screens.auth.AuthScreen
import org.youschool.app.ui.screens.auth.AuthViewModel
import org.youschool.app.ui.screens.grades.GradesScreen
import org.youschool.app.ui.screens.grades.GradesViewModel
import org.youschool.app.ui.screens.homework.HomeworkScreen
import org.youschool.app.ui.screens.homework.HomeworkViewModel
import org.youschool.app.ui.screens.profile.ProfileScreen
import org.youschool.app.ui.screens.schedule.ScheduleScreen
import org.youschool.app.ui.screens.schedule.ScheduleViewModel
import org.youschool.app.ui.theme.YouSchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as YouSchoolApp
        val repository = app.repository

        setContent {
            YouSchoolTheme {
                val navController = rememberNavController()
                val startDestination = if (repository.isUserLoggedIn()) Screen.Schedule.route else Screen.Auth.route

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != Screen.Auth.route) {
                            NavigationBar {
                                Screen.bottomNavScreens.forEach { screen ->
                                    NavigationBarItem(
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
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
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
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
        }
    }
}
