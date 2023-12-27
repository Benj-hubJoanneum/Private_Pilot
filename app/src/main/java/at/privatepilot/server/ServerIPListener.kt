package at.privatepilot.server

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ServerIPListener(private val context: Context) {

        private var currentInternetIP: String? = null
        private val handler = Handler(Looper.getMainLooper())

        init {
            startIPChangeChecker()
        }

        private fun startIPChangeChecker() {
            handler.postDelayed(object : Runnable {
                override fun run() {
                    checkInternetIP()
                    handler.postDelayed(this, IP_CHECK_INTERVAL)
                }
            }, IP_CHECK_INTERVAL)
        }

        private fun checkInternetIP() {
            GlobalScope.launch {
                val newInternetIP = NetworkManager.getInternetIpAddress()
                if (currentInternetIP == null || currentInternetIP != newInternetIP) {
                    currentInternetIP = newInternetIP
                    notifyClients(newInternetIP)
                }
            }
        }

        private fun notifyClients(newInternetIP: String) {
            val recipient = "lamprecht193@gmail.com"
            val subject = "Server IP Change Notification"
            val message = "The server's external IP address has changed. New IP: $newInternetIP" //currently hardcoded

            sendEmail(recipient, subject, message)
        }

        private fun sendEmail(toEmail: String, subject: String, message: String) {
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("text/plain");

            emailIntent.putExtra(Intent.EXTRA_EMAIL, toEmail)
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.putExtra(Intent.EXTRA_TEXT, message)

            try {
                context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        companion object {
            private const val IP_CHECK_INTERVAL = 60 * 30 * 1000L // Check every 30 minutes
        }
}