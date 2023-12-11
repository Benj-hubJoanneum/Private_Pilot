package at.privatepilot.server

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import javax.crypto.Cipher

object Encryption {
    private lateinit var privateKey: PrivateKey
    private lateinit var publicKey: PublicKey

    init {
        generateKeyPair()
    }

    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(4096)
        val keyPair = keyPairGenerator.generateKeyPair()

        privateKey = keyPair.private
        publicKey = keyPair.public
    }

    fun getPublicKey(): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun encrypt(originalMessage: String): String {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(originalMessage.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decrypt(encryptedMessage: String): String {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage))
        return String(decryptedBytes)
    }
}