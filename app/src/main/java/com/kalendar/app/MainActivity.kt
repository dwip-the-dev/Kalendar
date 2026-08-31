package com.kalendar.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kalendar.app.data.sync.DeviceCalendarManager
import com.kalendar.app.ui.navigation.KalendarNavigation
import com.kalendar.app.ui.navigation.Routes
import com.kalendar.app.ui.settings.SettingsViewModel
import com.kalendar.app.ui.splash.SplashScreen
import com.kalendar.app.ui.theme.KalendarTheme
import com.kalendar.app.util.DynamicIconManager
import com.kalendar.app.util.NotificationHelper
import com.kalendar.app.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object NavigationDispatcher {
    val targetRoute = MutableStateFlow<String?>(null)
}

/**
 * Single Activity for the entire Kalendar app.
 * Prompts for calendar and notification permissions on startup,
 * shows high quality animated splash screen,
 * auto-fetches Google accounts from device, and renders Compose UI.
 */
class MainActivity : ComponentActivity() {

    private var initialDestination: String = Routes.DAY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        parseIntentAction(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            var isSplashVisible by remember { mutableStateOf(true) }

            // Request calendar and notification permissions on first launch
            val permissionsToRequest = remember {
                val list = mutableListOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                list.toTypedArray()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val readGranted = permissions[Manifest.permission.READ_CALENDAR] == true
                if (readGranted) {
                    lifecycleScope.launch {
                        DeviceCalendarManager.syncDeviceCalendars(this@MainActivity)
                        WidgetUpdateHelper.updateAllWidgets(this@MainActivity)
                        NotificationHelper.rescheduleAllUpcomingReminders(this@MainActivity)
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (!DeviceCalendarManager.hasCalendarPermissions(this@MainActivity)) {
                    permissionLauncher.launch(permissionsToRequest)
                } else {
                    lifecycleScope.launch {
                        DeviceCalendarManager.syncDeviceCalendars(this@MainActivity)
                        WidgetUpdateHelper.updateAllWidgets(this@MainActivity)
                        NotificationHelper.rescheduleAllUpcomingReminders(this@MainActivity)
                    }
                }
            }

            KalendarTheme(themeMode = themeMode) {
                Crossfade(
                    targetState = isSplashVisible,
                    animationSpec = tween(durationMillis = 400),
                    label = "splashCrossfade"
                ) { showSplash ->
                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = { isSplashVisible = false }
                        )
                    } else {
                        KalendarNavigation(
                            settingsViewModel = settingsViewModel,
                            startDestination = initialDestination,
                            onThemeChanged = { settingsViewModel.setThemeMode(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure dynamic app icon and widgets are always synchronized with current date
        DynamicIconManager.updateAppIcon(this)
        DynamicIconManager.scheduleDailyMidnightUpdate(this)
        WidgetUpdateHelper.updateAllWidgets(this)
        NotificationHelper.rescheduleAllUpcomingReminders(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntentAction(intent)
    }

    private fun parseIntentAction(intent: Intent?) {
        val target = when (intent?.action) {
            WidgetUpdateHelper.ACTION_OPEN_DAY -> Routes.DAY_VIEW
            WidgetUpdateHelper.ACTION_OPEN_MONTH -> Routes.MONTH_VIEW
            WidgetUpdateHelper.ACTION_OPEN_EVENTS -> Routes.EVENTS_VIEW
            else -> null
        }
        if (target != null) {
            initialDestination = target
            NavigationDispatcher.targetRoute.value = target
        }
    }
}
