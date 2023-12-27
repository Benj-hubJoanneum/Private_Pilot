package at.privatepilot.server

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NetworkManager {
    suspend fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            // Format the IP address as a string
            InetAddress.getByAddress(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipAddress).array()
            ).hostAddress?.toString() ?: "no IP found"
        } catch (e: Exception) {
            // Handle exceptions, e.g., if there is no active connection
            "Failed to retrieve local IP"
        }
    }

    suspend fun getInternetIpAddress(): String {
        return try {
            val result: String

            withContext(Dispatchers.IO) {
                // Create an OkHttpClient instance
                val client = OkHttpClient()

                // Build the request
                val request = Request.Builder()
                    .url("https://api4.ipify.org?format=json")
                    .build()

                // Execute the request and get the response
                val response = client.newCall(request).execute()

                // Read the response body
                val jsonText = response.body?.string() ?: ""

                // Parse the JSON to get the IP address
                result = jsonText.substringAfter("\"ip\":\"").substringBefore("\"")
            }

            result
        } catch (e: Exception) {
            // Handle exceptions, e.g., network issues
            "Failed to retrieve internet IP"
        }
    }
}