package at.privatepilot.restapi.client

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import java.util.Base64

class WebSocketClient(private val callback: WebSocketCallback) {

    private val client = OkHttpClient()
    private val wsUrl = "ws://10.0.0.245:8080" // WebSocket URL
    private val publicKeyUrl = "http://10.0.0.245:8081/public-key" // Public key URL

    private var webSocket: WebSocket? = null
    private var reconnectionExecutor: ScheduledExecutorService? = null
    private val reconnectionDelay: Long = 2 // seconds

    private val credentialManager = CredentialManager.getInstance()

    private var crypt = CryptoUtils()

    private fun getConnection(): WebSocket {
        runBlocking {
            crypt.serverPublicKey = fetchServerPublicKey()
            if (webSocket == null || webSocket?.send("Ping") == false) {
                webSocket = createWebSocket(crypt.encrypt(credentialManager.name), crypt.encrypt(credentialManager.token))
            }
        }
        return webSocket as WebSocket
    }

    private fun createWebSocket(username: String, token: String): WebSocket {

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)

                webSocket.send("WebSocket connection opened")
                callback.onConnection()
                cancelReconnection()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                val decryptedMessage = crypt.decrypt(text)
                callback.onMessageReceived(decryptedMessage)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                println("Received bytes: ${bytes.utf8()}")
                callback.onMessageReceived(bytes)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                println("WebSocket connection closed. Code: $code, Reason: $reason")
                callback.onConnectionCancel()

                // Start reconnection mechanism
                scheduleReconnection()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                println("WebSocket connection failed: ${t.message}")
                callback.onConnectionFailure()

                // Start reconnection mechanism
                scheduleReconnection()
            }
        }

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("username", username)
            .addHeader("authorization", token)
            .addHeader("publickey", Base64.getEncoder().encodeToString(crypt.clientPublicKey?.encoded))
            .build()

        return client.newWebSocket(request, webSocketListener)
    }

    private fun getPublicKey(publicKeyPEM: String): PublicKey {
        try {
            val keyBytes = Base64.getDecoder().decode(publicKeyPEM.trimIndent()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", ""))
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            throw RuntimeException("Error building public key: ${e.message}")
        }
    }

    private fun fetchServerPublicKey(): PublicKey? {
        try {
            val url = URL(publicKeyUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val publicKeyPEM = reader.readText()

            return getPublicKey(publicKeyPEM)
        } catch (e: Exception) {
            println("Error fetching or building public key: ${e.message}")
        }
        return null
    }

    private fun cancelReconnection() {
        reconnectionExecutor?.shutdown()
    }

    private fun scheduleReconnection() {
        reconnectionExecutor?.shutdown()
        reconnectionExecutor = Executors.newSingleThreadScheduledExecutor()

        reconnectionExecutor?.scheduleAtFixedRate(
            { getConnection() },
            reconnectionDelay, reconnectionDelay, TimeUnit.SECONDS
        )
    }

    fun sendToServer(requestMessage: String) {
        getConnection().send(requestMessage)
    }

    fun sendToServer(requestMessage: ByteString) {
        getConnection().send(requestMessage)
    }

    interface WebSocketCallback {
        fun onMessageReceived(message: String)
        fun onMessageReceived(message: ByteString)

        fun onConnection()
        fun onConnectionCancel()
        fun onConnectionFailure()
    }
}
