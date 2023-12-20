package at.privatepilot.restapi.client

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

import java.util.Base64

class WebSocketClient(private val callback: WebSocketCallback) {

    private val client = OkHttpClient()
    private val wsUrl = "ws://10.0.0.99:3002/ws" // WebSocket URL

    private var webSocket: WebSocket? = null

    private val credentialManager = CredentialManager.getInstance()

    private lateinit var crypt: CryptoUtils

    private fun getConnection(): WebSocket {
        runBlocking {
            if (webSocket == null || webSocket?.send("Ping") == false) {
                crypt = CryptoUtils()
                webSocket = createWebSocket(
                    credentialManager.name, credentialManager.token)
                    //crypt.encrypt(credentialManager.name), crypt.encrypt(credentialManager.token))
            }
        }
        return webSocket as WebSocket
    }

    private fun createWebSocket(username: String, token: String): WebSocket {

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                callback.onConnection()
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
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                println("WebSocket connection failed: ${t.message}")
                callback.onConnectionFailure()
            }
        }

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("username", crypt.encrypt(username))
            .addHeader("authorization", crypt.encrypt(token))
            .addHeader("publickey", Base64.getEncoder().encodeToString(crypt.publicKey?.encoded)) //encrypt with public key
            .build()

        return client.newWebSocket(request, webSocketListener)
    }

    fun sendToServer(requestMessage: String, connection: WebSocket = getConnection()) {
        val encryptedMessage = crypt.encrypt(requestMessage)
        connection.send(encryptedMessage)
    }

    fun sendToServer(prefix: String, payload: ByteString) {
        val connection = getConnection()
        sendToServer(prefix, connection)
        val message = crypt.encrypt(payload)
        connection.send(message)
    }

    interface WebSocketCallback {
        fun onMessageReceived(message: String)
        fun onMessageReceived(message: ByteString)

        fun onConnection()
        fun onConnectionCancel()
        fun onConnectionFailure()
    }
}
