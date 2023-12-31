package at.privatepilot.client.restapi.client

import okio.ByteString
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
    private var serverPublicKey: PublicKey? = null
    var publicKey: PublicKey? = null
    private var privateKey: PrivateKey? = null

    init {
        generateKeyPair()
        serverPublicKey = fetchServerPublicKey("http://${NetworkRepository.keyServer}/public-key")
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
                cipher.init(Cipher.DECRYPT_MODE, privateKey)

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

    fun encrypt(originalMessage: ByteString): String {
        try {
            val maxChunkSize = 50
            val encodedMessage = originalMessage.toByteArray()
            val chunks = mutableListOf<ByteArray>()

            for (i in encodedMessage.indices step maxChunkSize) {
                val chunkSize = kotlin.math.min(maxChunkSize, encodedMessage.size - i)
                val chunk = encodedMessage.copyOfRange(i, i + chunkSize)
                chunks.add(chunk)
            }

            val encryptedChunks = mutableListOf<String>()

            for (chunk in chunks) {
                cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey)
                val encryptedBuffer = cipher.doFinal(chunk)
                val encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBuffer)
                encryptedChunks.add(encryptedBase64)
            }

            return encryptedChunks.joinToString("*")
        } catch (e: Exception) {
            println("Error during encryption: ${e.message}")
        }
        return ""
    }

    fun decrypt(plaintext: ByteString): ByteString {
        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey)

            val encryptedBytes = cipher.doFinal(plaintext.toByteArray())
            return ByteString.of(*encryptedBytes)
        } catch (e: Exception) {
            println("Error during encryption: ${e.message}")
        }
        return ByteString.EMPTY
    }

    private fun generateKeyPair() {
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()

            publicKey = keyPair.public
            privateKey = keyPair.private
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

    fun getPublicKeyFromString(base64EncodedPublicKey: String): PublicKey? {
        try {
            // Decode Base64-encoded public key
            val keyBytes: ByteArray =
                Base64.getDecoder().decode(base64EncodedPublicKey)

            // Create PublicKey instance
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun fetchServerPublicKey(publicKeyUrl: String): PublicKey? {
        try {
            val url = URL(publicKeyUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val publicKeyPEM = reader.readText()

            return getPublicKeyFromString(publicKeyPEM)
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
