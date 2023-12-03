package at.privatepilot.ui.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.privatepilot.databinding.RegisterBinding
import at.privatepilot.restapi.client.HttpClient
import at.privatepilot.restapi.client.CredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

            credentialManager.saveUserCredentials(this@RegisterActivity, username, password, serverAddress)
            credentialManager.updateCredentials(this@RegisterActivity)

            GlobalScope.launch(Dispatchers.IO) {
                httpClient.post("http://10.0.0.245:8081/register-user", username, credentialManager.token)
            }

            finish()
        }
    }
}