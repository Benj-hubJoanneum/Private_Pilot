package at.privatepilot.client.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import at.privatepilot.MainActivity
import at.privatepilot.client.restapi.client.CredentialManager
import at.privatepilot.client.restapi.client.HttpClient
import at.privatepilot.client.restapi.client.NetworkRepository
import at.privatepilot.databinding.LoginBinding
import at.privatepilot.server.ServerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private var credentialManager = CredentialManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.loginButton.setOnClickListener {
            val decryptedUsername = credentialManager.getStoredUsername(this@LoginActivity)
            val decryptedPassword = credentialManager.getStoredPassword(this@LoginActivity)
            val enteredUsername = binding.usernameEditText.text.toString()
            val enteredPassword = binding.passwordEditText.text.toString()

            if (enteredUsername == decryptedUsername && enteredPassword == decryptedPassword) {
                GlobalScope.launch(Dispatchers.IO) {
                    NetworkRepository.getServerIP(this@LoginActivity)

                    credentialManager.deviceauth = true
                    launchMainActivity()
                }
            }
        }

        binding.registerTextView.setOnClickListener {
            launchRegisterActivity()
        }

        binding.startServerTextView.setOnClickListener {
            launchServerActivity()
        }

    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun launchRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun launchServerActivity() {
        val intent = Intent(this, ServerActivity::class.java)
        startActivity(intent)
    }
}
