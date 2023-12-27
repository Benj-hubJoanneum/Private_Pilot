package at.privatepilot.client.restapi.client

class NetworkController {
    companion object {
        private var ipCallback: IpCallback? = null

        fun setIpCallback(callback: IpCallback) {
            ipCallback = callback
        }

        suspend fun getInternetIpAddress() {
            try {
                // Make your HTTP GET request to retrieve the IP
                val result = "Your logic to retrieve the IP address"

                ipCallback?.onIpReceived(result)
            } catch (e: Exception) {
                // Handle exceptions, e.g., network issues
                ipCallback?.onIpReceived("Failed to retrieve internet IP")
            }
        }
    }

    interface IpCallback {
        fun onIpReceived(ip: String)
    }
}