package at.privatepilot.client.restapi.client

import android.content.Context

object CredentialManager {

    var name = ""
    var token = ""
    var deviceauth = false
    var lan = ""
    var wan = ""
    var port = 3000

    private const val PREFERENCES_NAME = "UserCredentials"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SERVER_IP_WAN = "wanIP"
    private const val KEY_SERVER_IP_LAN = "lanIP"
    private const val KEY_HOME_NETWORK_SSID = "homeNetworkSSID"
    private const val KEY_PORT = "port"

    @Volatile
    private var instance: CredentialManager? = null

    fun getInstance(): CredentialManager =
        instance ?: synchronized(this) {
            instance ?: CredentialManager.also { instance = it }
        }

    fun saveUserCredentials(
        context: Context,
        username: String?,
        password: String?,
        serverAddress: String?,
        port: Int,
        homeNetworkSSID: String
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_PASSWORD, password)
        editor.putString(KEY_SERVER_IP_LAN, serverAddress)
        editor.putInt(KEY_PORT, port)
        editor.putString(KEY_HOME_NETWORK_SSID, homeNetworkSSID)
        editor.apply()
    }

    fun saveServerAddress(
        context: Context,
        serverIpWAN: String?,
        serverIpLAN: String?,
        port: String
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(KEY_SERVER_IP_WAN, serverIpWAN)
        editor.putString(KEY_SERVER_IP_LAN, serverIpLAN)
        editor.putString(KEY_PORT, port)
        editor.apply()
        wan = getStoredServerWANAddress(context) ?: ""
    }

    fun saveWANAddress(
        context: Context,
        serverIpWAN: String?
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(KEY_SERVER_IP_WAN, serverIpWAN)
        editor.apply()
        wan = getStoredServerWANAddress(context) ?: ""
    }

    fun updateCredentials(context: Context){
        name = getStoredUsername(context) ?: ""
        token = StringBuilder()
            .append(getStoredPassword(context))
            .append(DeviceInfoUtil(context).getPhoneNumber())
            .append(DeviceInfoUtil(context).getMacAddress())
            .toString()
        lan = getStoredServerLANAddress(context) ?: ""
        port = getStoredPort(context) ?: 3000
    }

    fun getStoredUsername(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_USERNAME, null)
    }

    fun getStoredPassword(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_PASSWORD, null)
    }

    fun getStoredServerLANAddress(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_SERVER_IP_LAN, null)
    }

    fun getStoredServerWANAddress(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_SERVER_IP_WAN, null)
    }

    fun getHomeNetworkSSID(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_HOME_NETWORK_SSID, null)
    }

    fun getStoredPort(context: Context): Int {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getInt(KEY_PORT, 3000)
    }

}
