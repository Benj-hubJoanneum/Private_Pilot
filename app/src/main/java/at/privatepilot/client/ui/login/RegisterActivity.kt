package at.privatepilot.client.ui.login

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.privatepilot.client.restapi.client.CredentialManager
import at.privatepilot.client.restapi.client.HttpClient
import at.privatepilot.client.restapi.client.NetworkRepository
import at.privatepilot.databinding.RegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: RegisterBinding
    private val httpClient = HttpClient()
    private val credentialManager = CredentialManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = RegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val serverAddress = binding.serverAddressEditText.text.toString()
            val port = binding.registerPort.text.toString().toInt()

            credentialManager.saveUserCredentials(this@RegisterActivity, username, password, serverAddress, port, NetworkRepository.getHomeNetworkSSID(this))
            credentialManager.updateCredentials(this@RegisterActivity)

            GlobalScope.launch(Dispatchers.IO) {
                NetworkRepository.getServerIP(this@RegisterActivity)
                httpClient.post("http://${NetworkRepository.registerServer}/register-user", username, getLocalIpAddress(), credentialManager.token)
            }



            finish()
        }
    }

    private fun getLocalIpAddress(): String {
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
}
