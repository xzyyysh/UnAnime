package com.unanimetv.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.unanimetv.android.core.designsystem.theme.ThemeController
import com.unanimetv.android.core.designsystem.theme.UnAnimeTheme
import com.unanimetv.android.core.network.ApiClient
import com.unanimetv.android.core.update.UpdateChecker
import com.unanimetv.android.core.update.UpdateNotifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        ApiClient.init(this)
        handleUpdateNotificationIntent(intent)
        setContent {
            val themeSettings by ThemeController.settings.collectAsState()
            UnAnimeTheme(themeSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        UnAnimeApp()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUpdateNotificationIntent(intent)
    }

    private fun handleUpdateNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateNotifier.EXTRA_FROM_NOTIFICATION, false) == true) {
            UpdateChecker.markNotificationOpen()
        }
    }
}
