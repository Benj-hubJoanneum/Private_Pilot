package at.privatepilot.client.restapi.client

import android.Manifest
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import at.privatepilot.client.ui.login.RegisterActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.errors.IOException
import java.net.InetAddress

object NetworkRepository {

    var registerServer = ""
    var keyServer = ""
    var websocketServer = ""
    val credentialManager = CredentialManager.getInstance()

    val httpClient = HttpClient()

    fun useWAN(context: Context) {
        try {
            credentialManager.updateCredentials(context)
            val wan = credentialManager.getStoredServerWANAddress(context)

            registerServer = "${wan}:${credentialManager.port + 2}"
            keyServer = "${wan}:${credentialManager.port + 1}"
            websocketServer = "${wan}:${credentialManager.port}"

        } catch (e: Exception) {
            Log.d("Error", "Error with the IP address")
        }
    }

    fun useLAN(context: Context) {
        try {
            credentialManager.updateCredentials(context)
            try {
                if (credentialManager.getHomeNetworkSSID(context) == getHomeNetworkSSID(context)) {
                    val response = httpClient.get("http://${credentialManager.lan}:${credentialManager.port + 1}/ip")
                    registerServer = "${credentialManager.lan}:${credentialManager.port + 2}"
                    keyServer = "${credentialManager.lan}:${credentialManager.port + 1}"
                    websocketServer = "${credentialManager.lan}:${credentialManager.port}"

                    credentialManager.saveWANAddress(context, response)
                }
            } catch (e: Exception) {
                Log.d("Error", "Error with the WAN IP")
            }
        } catch (e: Exception) {
            Log.d("Error", "Error with the IP address")
        }
    }

    fun getHomeNetworkSSID(context: Context): String {
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager

        // Check if Wi-Fi is enabled
        if (!wifiManager.isWifiEnabled) {
            return "Wi-Fi is not enabled"
        }

        // Check if the necessary permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "Missing ACCESS_WIFI_STATE permission"
        }

        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo.bssid == null || wifiInfo.bssid == "02:00:00:00:00:00") {
            return "Not connected to a Wi-Fi network"
        }

        // Retrieve the BSSID
        return wifiInfo.bssid
    }

    fun setWANIP(ip: String, context: Context) {
        CredentialManager.saveWANAddress(context, ip)
    }

    private suspend fun isServerReachable(serverAddress: String, timeout: Int): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val address = java.net.InetAddress.getByName(serverAddress)
                address.isReachable(timeout)
            }
        } catch (e: IOException) {
            false
        }
    }
}
