package at.privatepilot.restapi.client

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

class CryptoUtils {

    private val cipher: Cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    var serverPublicKey: PublicKey? = null
    var clientPublicKey: PublicKey? = null
    private var clientPrivateKey: PrivateKey? = null

    init {
        generateKeyPair()
        serverPublicKey = fetchServerPublicKey("http://10.0.0.245:8081/public-key")
    }

    fun encrypt(plaintext: String): String {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey)

            val encryptedBytes = cipher.doFinal(plaintext.toByteArray())
            return Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            println("Error during encryption: ${e.message}")
        }
        return ""
    }

    fun decrypt(encryptedMessage: String): String {
        try {
            val encryptedChunks = encryptedMessage.split("*")

            val decryptedChunks = mutableListOf<String>()

            for (encryptedChunk in encryptedChunks) {
                val encryptedBytes = Base64.getDecoder().decode(encryptedChunk)
                val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
                cipher.init(Cipher.DECRYPT_MODE, clientPrivateKey)

                val decryptedBytes = cipher.doFinal(encryptedBytes)

                decryptedChunks.add(String(decryptedBytes))
            }

            return decryptedChunks.joinToString("")
        } catch (e: Exception) {
            println("Error during decryption: ${e.message}")
            // Handle the error appropriately
        }
        return ""
    }

    private fun generateKeyPair() {
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()

            clientPublicKey = keyPair.public
            clientPrivateKey = keyPair.private
        } catch (e: Exception) {
            println("Error generating key pair: ${e.message}")
        }
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

    fun fetchServerPublicKey(publicKeyUrl: String): PublicKey? {
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

    private fun encrypt(plaintext: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey)
        return cipher.doFinal(plaintext)
    }
}
