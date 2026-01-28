package com.noxvision.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.widget.Toast
import com.noxvision.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WiFiAutoConnect(
    private val context: Context,
    private val cameraSSID: String,
    private val cameraPassword: String
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    suspend fun connectToCamera(): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("Connecting to $cameraSSID...", AppLogger.LogType.INFO)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectModern()
            } else {
                AppLogger.log(
                    "Android 9: Please connect manually to $cameraSSID",
                    AppLogger.LogType.INFO
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Please connect manually to $cameraSSID",
                        Toast.LENGTH_LONG
                    ).show()
                }
                false
            }
        } catch (e: Exception) {
            AppLogger.log("WiFi Error: ${e.message}", AppLogger.LogType.ERROR)
            false
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun connectModern(): Boolean = withContext(Dispatchers.Main) {
        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(cameraSSID)
                .setWpa2Passphrase(cameraPassword)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            var connected = false

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.bindProcessToNetwork(network)
                    connected = true
                    AppLogger.log("WiFi connected: $cameraSSID", AppLogger.LogType.SUCCESS)
                }

                override fun onUnavailable() {
                    AppLogger.log("WiFi not available", AppLogger.LogType.ERROR)
                }
            }

            connectivityManager.requestNetwork(request, networkCallback!!)

            var attempts = 0
            while (!connected && attempts < 100) {
                delay(500)
                attempts++
                if (attempts % 4 == 0) {
                    AppLogger.log("Waiting for connection... (${attempts / 2}s)", AppLogger.LogType.INFO)
                }
            }

            if (connected) {
                delay(1000)
            } else {
                AppLogger.log("Timeout after ${attempts / 2}s", AppLogger.LogType.ERROR)
            }

            connected
        } catch (e: Exception) {
            AppLogger.log("WiFi Error: ${e.message}", AppLogger.LogType.ERROR)
            false
        }
    }

    fun disconnect() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                networkCallback?.let {
                    connectivityManager.unregisterNetworkCallback(it)
                    connectivityManager.bindProcessToNetwork(null)
                }
                networkCallback = null
            }
            AppLogger.log("WiFi disconnected", AppLogger.LogType.INFO)
        } catch (e: Exception) {
            AppLogger.log("Disconnect Error: ${e.message}", AppLogger.LogType.ERROR)
        }
    }
}
