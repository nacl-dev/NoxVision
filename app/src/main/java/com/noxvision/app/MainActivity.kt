package com.noxvision.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import com.noxvision.app.network.WiFiAutoConnect
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.VideoStreamScreen

class MainActivity : ComponentActivity() {

    internal var wifiAutoConnect: WiFiAutoConnect? = null

    internal fun updateWifiAutoConnect() {
        val ssid = CameraSettings.getWifiSsid(this)
        val password = CameraSettings.getWifiPassword(this)
        wifiAutoConnect = WiFiAutoConnect(this, ssid, password)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateWifiAutoConnect()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = NightColors.background,
                    surface = NightColors.surface,
                    primary = NightColors.primary,
                    onBackground = NightColors.onBackground,
                    onSurface = NightColors.onSurface
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NightColors.background
                ) {
                    VideoStreamScreen()
                }
            }
        }
    }
}
