package com.civicresolve.ap

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.civicresolve.ap.ui.navigation.AppNavigation
import com.civicresolve.ap.ui.theme.CivicResolveTheme

class MainActivity : AppCompatActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.civicresolve.ap.notification.NotificationSocketManager.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ctx = applicationContext
        org.osmdroid.config.Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "com.civicresolve.ap/1.0"

        enableEdgeToEdge()

        val appContainer = (application as CivicResolveApplication).container

        setContent {
            val themeMode by appContainer.themePreferences.themeModeFlow.collectAsState(initial = com.civicresolve.ap.ui.theme.ThemeMode.SYSTEM)
            CivicResolveTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(appContainer = appContainer)
                }
            }
        }
    }
}
