package at.privatepilot.restapi.client

import android.content.Context
import android.content.ContextWrapper
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.util.Log

class DeviceInfoUtil(context: Context) : ContextWrapper(context) {

    private val telephonyManager: TelephonyManager by lazy {
        getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    private val wifiManager: WifiManager by lazy {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun getPhoneNumber(): String? {
        return try {
            telephonyManager.line1Number
        } catch (e: SecurityException) {
            Log.e("DeviceInfoUtil", "Permission READ_PHONE_STATE not granted")
            null
        }
    }

    fun getMacAddress(): String? {
        return try {
            val wifiInfo: WifiInfo? = wifiManager.connectionInfo
            wifiInfo?.macAddress
        } catch (e: SecurityException) {
            Log.e("DeviceInfoUtil", "Permission ACCESS_WIFI_STATE not granted")
            null
        }
    }
}
