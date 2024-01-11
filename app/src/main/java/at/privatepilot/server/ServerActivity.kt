package at.privatepilot.server

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import at.privatepilot.client.ui.login.LoginActivity
import at.privatepilot.databinding.ActivityServerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ServerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerBinding
    private var websocketServer: Websocket? = null
    private var httpServer: Http? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IPListenerService(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel = ViewModelProvider(this)[ServerViewModel::class.java]
        val encryption = Encryption()

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Launch a coroutine to update addresses asynchronously
        GlobalScope.launch(Dispatchers.Main) {
            val localIpAddress = NetworkManager.getLocalIpAddress(this@ServerActivity)
            val internetIpAddress = NetworkManager.getInternetIpAddress()

            viewModel.updateAddresses(localIpAddress, internetIpAddress)
        }

        binding.stopServerButton.setOnClickListener {
            stopServer()
        }

        GlobalScope.launch(Dispatchers.IO) {
            httpServer = Http(3001, 3002, encryption, this@ServerActivity)
        }

        GlobalScope.launch(Dispatchers.IO) {
            websocketServer = Websocket(3000, encryption, this@ServerActivity)
        }

        binding.registerUserSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                httpServer?.startRegisterUserServer()
            } else {
                httpServer?.stopRegisterUserServer()
            }
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
}
