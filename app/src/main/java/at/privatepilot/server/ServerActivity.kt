package at.privatepilot.server

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import at.privatepilot.databinding.ActivityServerBinding
import at.privatepilot.client.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ServerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerBinding
    private var websocketServer: Websocket? = null
    private var httpServer: Http? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel = ViewModelProvider(this)[ServerViewModel::class.java]
        val encryption = Encryption()

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Launch a coroutine to update addresses asynchronously
        GlobalScope.launch(Dispatchers.Main) {
            val localIpAddress = getLocalIpAddress()
            val internetIpAddress = getInternetIpAddress()

            viewModel.updateAddresses(localIpAddress, internetIpAddress)
        }

        binding.stopServerButton.setOnClickListener {
            stopServer()
        }

        GlobalScope.launch(Dispatchers.IO) {
            httpServer = Http(3001, encryption, this@ServerActivity)
        }

        GlobalScope.launch(Dispatchers.IO) {
            websocketServer = Websocket(3002, encryption, this@ServerActivity)
        }
    }

    private fun stopServer() {
        // Stop WebSocket server
        websocketServer?.let {
            // Add any cleanup logic if needed
        }

        // Stop HTTP server
        httpServer?.let {
            // Add any cleanup logic if needed
        }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    private suspend fun getLocalIpAddress(): String {
        return try {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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

    private suspend fun getInternetIpAddress(): String {
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
