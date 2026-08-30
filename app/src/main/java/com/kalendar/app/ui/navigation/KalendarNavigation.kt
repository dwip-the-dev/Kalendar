package com.kalendar.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kalendar.app.ui.calculatedate.CalculateDateScreen
import com.kalendar.app.ui.components.KalendarBottomNavBar
import com.kalendar.app.ui.dayview.DayViewScreen
import com.kalendar.app.ui.dayview.DayViewViewModel
import com.kalendar.app.ui.eventview.*
import com.kalendar.app.ui.monthview.MonthViewScreen
import com.kalendar.app.ui.monthview.MonthViewViewModel
import com.kalendar.app.ui.settings.ManageAccountsScreen
import com.kalendar.app.ui.settings.SettingsScreen
import com.kalendar.app.ui.settings.SettingsViewModel
import com.kalendar.app.ui.theme.ThemeMode
import com.kalendar.app.ui.yearview.YearViewScreen

object Routes {
    const val DAY_VIEW = "day_view"
    const val EVENTS_VIEW = "events_view"
    const val MONTH_VIEW = "month_view"
    const val YEAR_VIEW = "year_view"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val EVENT_CREATE = "event_create"
    const val EVENT_EDIT = "event_edit/{eventId}"
    const val SETTINGS = "settings"
    const val MANAGE_ACCOUNTS = "manage_accounts"
    const val CALCULATE_DATE = "calculate_date"
    const val SEARCH_EVENTS = "search_events"

    fun eventDetail(eventId: Long) = "event_detail/$eventId"
    fun eventEdit(eventId: Long) = "event_edit/$eventId"
}

@Composable
fun KalendarNavigation(
    settingsViewModel: SettingsViewModel,
    startDestination: String = Routes.DAY_VIEW,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val dayViewViewModel: DayViewViewModel = viewModel()
    val monthViewViewModel: MonthViewViewModel = viewModel()
    val eventViewModel: EventViewModel = viewModel()

    // 4 primary tabs from bottom nav bar: Year, Month, Event, Day
    val showBottomNav = currentRoute in listOf(
        Routes.YEAR_VIEW, Routes.MONTH_VIEW, Routes.EVENTS_VIEW, Routes.DAY_VIEW
    )

    val currentSelectedDate by dayViewViewModel.uiState.collectAsState()
    val externalTargetRoute by com.kalendar.app.NavigationDispatcher.targetRoute.collectAsState()

    LaunchedEffect(externalTargetRoute) {
        externalTargetRoute?.let { route ->
            if (route != currentRoute) {
                navController.navigate(route) {
                    popUpTo(Routes.DAY_VIEW) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            com.kalendar.app.NavigationDispatcher.targetRoute.value = null
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                KalendarBottomNavBar(
                    currentRoute = currentRoute ?: Routes.DAY_VIEW,
                    currentDate = currentSelectedDate.selectedDate,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.DAY_VIEW) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            // Day View (Mockup #1 & #5)
            composable(Routes.DAY_VIEW) {
                DayViewScreen(
                    viewModel = dayViewViewModel,
                    onNavigateToSearch = {
                        navController.navigate(Routes.SEARCH_EVENTS)
                    },
                    onNavigateToCalculateDate = {
                        navController.navigate(Routes.CALCULATE_DATE)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDetail(eventId))
                    },
                    onCreateEvent = { prefillDate ->
                        eventViewModel.initCreateEvent(prefillDate)
                        navController.navigate(Routes.EVENT_CREATE)
                    }
                )
            }

            // Events List View (Tab 3: Event)
            composable(Routes.EVENTS_VIEW) {
                EventsListScreen(
                    viewModel = eventViewModel,
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDetail(eventId))
                    },
                    onCreateEvent = {
                        eventViewModel.initCreateEvent()
                        navController.navigate(Routes.EVENT_CREATE)
                    }
                )
            }

            // Month View (Screenshot #3)
            composable(Routes.MONTH_VIEW) {
                MonthViewScreen(
                    viewModel = monthViewViewModel,
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDetail(eventId))
                    },
                    onCreateEvent = { prefillDate ->
                        eventViewModel.initCreateEvent(prefillDate)
                        navController.navigate(Routes.EVENT_CREATE)
                    }
                )
            }

            // Year View (Screenshot #4)
            composable(Routes.YEAR_VIEW) {
                YearViewScreen(
                    onMonthSelected = { yearMonth ->
                        monthViewViewModel.setYearMonth(yearMonth)
                        navController.navigate(Routes.MONTH_VIEW) {
                            launchSingleTop = true
                        }
                    },
                    onDateSelected = { date ->
                        dayViewViewModel.selectDate(date)
                        monthViewViewModel.selectDate(date)
                        monthViewViewModel.setYearMonth(java.time.YearMonth.from(date))
                        navController.navigate(Routes.DAY_VIEW) {
                            popUpTo(Routes.DAY_VIEW) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateEvent = {
                        eventViewModel.initCreateEvent()
                        navController.navigate(Routes.EVENT_CREATE)
                    }
                )
            }

            // Calculate Date Screen (Mockup #4)
            composable(Routes.CALCULATE_DATE) {
                CalculateDateScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings Screen (Mockup #2)
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onManageAccountsClick = {
                        navController.navigate(Routes.MANAGE_ACCOUNTS)
                    }
                )
            }

            // Manage Accounts Screen (Mockup #3)
            composable(Routes.MANAGE_ACCOUNTS) {
                ManageAccountsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Search Events Screen
            composable(Routes.SEARCH_EVENTS) {
                SearchEventsScreen(
                    viewModel = eventViewModel,
                    onBack = { navController.popBackStack() },
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDetail(eventId))
                    }
                )
            }

            // Event Detail
            composable(
                route = Routes.EVENT_DETAIL,
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getLong("eventId") ?: return@composable
                EventDetailScreen(
                    eventId = eventId,
                    viewModel = eventViewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { id ->
                        eventViewModel.initEditEvent(id)
                        navController.navigate(Routes.eventEdit(id))
                    },
                    onDeleted = { navController.popBackStack() }
                )
            }

            // Event Create (Screenshot #2)
            composable(Routes.EVENT_CREATE) {
                EventEditScreen(
                    viewModel = eventViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Event Edit
            composable(
                route = Routes.EVENT_EDIT,
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) {
                EventEditScreen(
                    viewModel = eventViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
