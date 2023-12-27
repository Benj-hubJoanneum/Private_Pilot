package at.privatepilot.client.restapi.client

import android.content.Context
import android.util.Log
import at.privatepilot.client.ui.login.RegisterActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NetworkRepository {

    var registerServer = ""
    var keyServer = ""
    var websocketServer = ""
    val credentialManager = CredentialManager.getInstance()

    fun getServerIP(context: Context) {
        credentialManager.updateCredentials(context)
        if(credentialManager.getHomeNetworkSSID(context) == getHomeNetworkSSID(context)){
            registerServer = "${credentialManager.lan}:${credentialManager.port + 2}"
            keyServer = "${credentialManager.lan}:${credentialManager.port + 1}"
            websocketServer = "${credentialManager.lan}:${credentialManager.port}"

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = HttpClient().get("http://${NetworkRepository.keyServer}/ip")
                    credentialManager.saveWANAddress(context, response)
                } catch (e: Exception) {
                    Log.d("Error", "couldn't call WAN ip")
                }
            }

        } else {
            registerServer = "${credentialManager.wan}:${credentialManager.port + 2}"
            keyServer = "${credentialManager.wan}:${credentialManager.port + 1}"
            websocketServer = "${credentialManager.wan}:${credentialManager.port}"
        }
    }

    fun getHomeNetworkSSID(context: Context): String {
        val wifiInfo = (context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager).connectionInfo
        return wifiInfo.ssid
    }

    fun setWANIP(ip: String, context: Context) {
        CredentialManager.saveWANAddress(context, ip)
    }
}
