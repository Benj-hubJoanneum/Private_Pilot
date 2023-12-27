package at.privatepilot.client.restapi.client

import android.content.Context
import at.privatepilot.client.ui.login.RegisterActivity

object NetworkRepository {

    var registerServer = ""
    var keyServer = ""
    var websocketServer = ""

    fun getServerIP(context: Context) {
        CredentialManager.updateCredentials(context)
        if(CredentialManager.getHomeNetworkSSID(context) == getHomeNetworkSSID(context)){
            registerServer = "${CredentialManager.lan}:${CredentialManager.port + 2}"
            keyServer = "${CredentialManager.lan}:${CredentialManager.port + 1}"
            websocketServer = "${CredentialManager.lan}:${CredentialManager.port}"
        } else {
            registerServer = "${CredentialManager.wan}:${CredentialManager.port + 2}"
            keyServer = "${CredentialManager.wan}:${CredentialManager.port + 1}"
            websocketServer = "${CredentialManager.wan}:${CredentialManager.port}"
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
