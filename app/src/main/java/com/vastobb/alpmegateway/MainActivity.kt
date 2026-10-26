package com.vastobb.alpmegateway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vastobb.alpmegateway.ui.GatewayApp
import com.vastobb.alpmegateway.ui.GatewayViewModel
import com.vastobb.alpmegateway.ui.theme.AlpmeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GatewayViewModel by viewModels {
        GatewayViewModel.factory((application as AlpmeApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            AlpmeTheme(themeMode = settings.theme) {
                GatewayApp(viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4107)
            }
        }
    }
}
