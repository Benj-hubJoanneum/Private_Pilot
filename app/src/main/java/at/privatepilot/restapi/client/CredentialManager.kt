package at.privatepilot.restapi.client

import android.content.Context

object CredentialManager {

    var name = ""
    var token = ""

    private const val PREFERENCES_NAME = "UserCredentials"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SERVER_IP_WAN = "ws://10.0.0.245:8080"
    private const val KEY_SERVER_IP_LAN = "ws://10.0.0.245:8090"
    private const val KEY_MAC = "ws://10.0.0.245:8090"
    private const val KEY_SIM = "ws://10.0.0.245:8090"

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
        serverAddress: String?
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_PASSWORD, password)
        editor.putString(KEY_SERVER_IP_LAN, serverAddress)
        editor.apply()
    }

    fun updateCredentials(context: Context){
        name = getStoredUsername(context) ?: ""
        token = StringBuilder()
            .append(getStoredPassword(context))
            .append(DeviceInfoUtil(context).getPhoneNumber())
            .append(DeviceInfoUtil(context).getMacAddress())
            .toString()
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
        return preferences.getString(KEY_SERVER_IP_WAN, null)
    }

    fun getStoredServerWANAddress(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_SERVER_IP_LAN, null)
    }

    fun clearUserCredentials(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.remove(KEY_USERNAME)
        editor.remove(KEY_PASSWORD)
        editor.remove(KEY_SERVER_IP_WAN)
        editor.remove(KEY_SERVER_IP_LAN)
        editor.apply()
    }
}
