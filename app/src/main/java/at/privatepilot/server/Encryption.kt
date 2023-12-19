package at.privatepilot.server

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import javax.crypto.Cipher

import okio.ByteString
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

class Encryption {

    private val cipher: Cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    private var clientPublicKey: PublicKey? = null
    var publicKey: PublicKey? = null
    private var privateKey: PrivateKey? = null

    init {
        generateKeyPair()
    }

    fun decrypt(encryptedMessage: String): String {
        try {
            val encryptedChunks = encryptedMessage.split("*")
            val decryptedChunks = mutableListOf<String>()

            for (encryptedChunk in encryptedChunks) {
                val encryptedBytes = Base64.getDecoder().decode(encryptedChunk)
                cipher.init(Cipher.DECRYPT_MODE, privateKey)

                val decryptedBytes = cipher.doFinal(encryptedBytes)

                decryptedChunks.add(String(decryptedBytes))
            }

            return decryptedChunks.joinToString("")
        } catch (e: Exception) {
            println("Error during decryption: ${e.message}")
        }
        return ""
    }

    fun decryptFile(encryptedFile: String): ByteString {
        try {
            val encryptedChunks = encryptedFile.split("*")
            val decryptedChunks = mutableListOf<ByteArray>()

            for (encryptedChunk in encryptedChunks) {
                val encryptedBytes = Base64.getDecoder().decode(encryptedChunk)
                cipher.init(Cipher.DECRYPT_MODE, privateKey)

                val decryptedBuffer = cipher.doFinal(encryptedBytes)
                decryptedChunks.add(decryptedBuffer)
            }

            val decryptedBytes = decryptedChunks.flatMap { it.toList() }.toByteArray()
            return ByteString.of(*decryptedBytes)
        } catch (e: Exception) {
            println("Error during file decryption: ${e.message}")
            return ByteString.EMPTY
        }
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
                cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey)
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

    fun encrypt(originalMessage: String): String {
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
                cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey)
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

    fun getPublicKeyFromString(base64EncodedPublicKey: String?): PublicKey? {
        try {
            val keyBytes: ByteArray =
                Base64.getDecoder().decode(base64EncodedPublicKey)

            // Create PublicKey instance
            val keyFactory = KeyFactory.getInstance("RSA")
            clientPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getPublicKey(): String {
        try {
            return Base64.getEncoder().encodeToString(publicKey?.encoded)
        } catch (e: Exception) {
            throw RuntimeException("Error building public key: ${e.message}")
        }
    }
}