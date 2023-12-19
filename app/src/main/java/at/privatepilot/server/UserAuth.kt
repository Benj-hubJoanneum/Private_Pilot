package at.privatepilot.server

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class UserAuth(val context: Context) {

    private val gson = Gson()

    fun registerUser(name: String, token: String): Boolean {
        try {
            val salt = generateSalt()
            val hashedToken = hashPassword(token, salt)

            val user = User(name, Token(hashedToken, salt))

            val usersData = getUsersData()
            if (usersData.users.any { it.name == name }) {
                println("Username already taken")
                return false
            }

            usersData.users.add(user)
            saveUsersData(usersData.users)

            println("User ${user.name} registered successfully")
            return true
        } catch (error: Exception) {
            println("Error reading or parsing users data: $error")
            return false
        }
    }

    fun validateUser(name: String?, token: String?): Boolean {
        try {
            if (name != null && token != null) {
                val usersData = getUsersData()
                val user = usersData.users.find { it.name == name } ?: return false

                val storedHashedToken = user.token.hashed
                val salt = user.token.salt
                val hashedTokenToValidate = hashPassword(token, salt)

                if (hashedTokenToValidate != storedHashedToken) {
                    println("Invalid token for user: $name")
                    return false
                }

                return true
            }
            return false
        } catch (error: Exception) {
            println("Error reading or parsing users data: $error")
            return false
        }
    }

    private fun getUsersData(): Users {
        val file = getUsersFile()

        return try {
            if (file.exists()) {
                val fileContent = file.readText(Charsets.UTF_8)
                Gson().fromJson(fileContent, Users::class.java)
            } else {
                Users(mutableListOf()) // Return an empty Users object if the file doesn't exist
            }
        } catch (e: Exception) {
            println("Error reading or parsing users data: $e")
            Users(mutableListOf()) // Return an empty Users object in case of an error
        }
    }

    private fun saveUsersData(usersData: List<User>) {
        try {
            val jsonString = gson.toJson(usersData)
            val usersFile = getUsersFile()
            usersFile.writeText(jsonString, Charsets.UTF_8)
            println("User data saved successfully")
        } catch (e: Exception) {
            println("Error saving users data: $e")
        }
    }

    private fun getUsersFile(): File {
        val dirPath = "users"
        val fileName = "users.json"
        val directory = context.getExternalFilesDir(dirPath)

        if (directory != null) {
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    println("Error creating directory: $dirPath")
                }
            }
            return File(directory, fileName)
        } else {
            println("External files directory is null")
            return File(dirPath, fileName)
        }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = password + salt
        val hashBytes = digest.digest(saltedPassword.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
