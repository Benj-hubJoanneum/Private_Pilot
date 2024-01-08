package at.privatepilot.server

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import at.privatepilot.server.model.Metadata
import at.privatepilot.server.model.MetadataResponse
import com.google.gson.Gson
import io.ktor.application.install
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import java.util.Base64


class Websocket(private val port: Int, private val encryption: Encryption, private val context: Context) {
    private val BASE_DIRECTORY = "server_"
    private val home = context.getExternalFilesDir(BASE_DIRECTORY)
    private var pointer = BASE_DIRECTORY

    private val controller = Controller(context)

    init {
        val userAuth = UserAuth(context)
        embeddedServer(Netty, port) {
            install(io.ktor.websocket.WebSockets)
            routing {
                webSocket("/ws") {
                    controller.fileExist(BASE_DIRECTORY)
                    val username = call.request.headers["username"]
                    val authorization = call.request.headers["authorization"]
                    val publickey = call.request.headers["publickey"]
                    encryption.getPublicKeyFromString(publickey)
                    val decodedUsername = username?.let { encryption.decrypt(it) }
                    val decodedauthorization = authorization?.let { encryption.decrypt(it) }
                    userAuth.validateUser(decodedUsername, decodedauthorization)

                    incoming.consumeEach { frame ->
                        when (frame) {
                            is Frame.Text -> {
                                var decryptedMessage = frame.readText()

                                if (decryptedMessage != "Ping") {
                                    decryptedMessage = encryption.decrypt(decryptedMessage)

                                    when {
                                        decryptedMessage.startsWith("GET:") -> handleGetRequest(this, decryptedMessage.substring(4))
                                        decryptedMessage.startsWith("POST:") -> handlePostRequest(this, decryptedMessage.substring(5))
                                        decryptedMessage.startsWith("DELETE:") -> handleDeleteRequest(this,decryptedMessage.substring(8))
                                        decryptedMessage.startsWith("FIND:") -> handleSearchRequest(this, decryptedMessage.substring(5))
                                        decryptedMessage.startsWith("UPDATE:") -> handleUpdateRequest(this, decryptedMessage.substring(7))
                                    }
                                }
                            }
                            // Handle other types of frames if needed
                            else -> {outgoing.send(Frame.Text("toast; request not recognized"))}
                        }
                    }
                }
            }
        }.start(wait = true)
    }

    private suspend fun sendText(session: DefaultWebSocketSession, message: String) {
        try {
            val encryptedMessage = encryption.encrypt(message)
            session.outgoing.send(Frame.Text(encryptedMessage))
            println("Sent message: $message")
        } catch (e: Exception) {
            println("Error sending WebSocket message: ${e.message}")
        }
    }

    private suspend fun sendFileToClient(session: DefaultWebSocketSession, filePath: String) {
        try {
            val fileData = File(filePath).readBytes().toByteString()
            val encryptedFile = encryption.encrypt(fileData)
            session.outgoing.send(Frame.Text(encryptedFile))
            println("Sent file: $filePath")
        } catch (e: Exception) {
            println("Error sending file: ${e.message}")
        }
    }

    private suspend fun handleGetRequest(session: DefaultWebSocketSession, request: String) {
        try {
            val requestedFile = controller.fileExist(request)

            if (requestedFile.exists()) {
                if (requestedFile.isDirectory) {

                    pointer = home?.let { requestedFile.relativeTo(it).path }.toString()

                    val items = listFileMetadata(requestedFile)
                    // Send the response with file metadata
                    sendText(session, "$request;${Gson().toJson(items)}")
                    sendFilePreviews(session, items)
                } else if (requestedFile.isFile) {
                    // Print a message for debugging
                    println("Sending file: $requestedFile")

                    // Send the file to the client
                    sendFileToClient(session, requestedFile.absolutePath)
                }
            } else {
                // Print a message for debugging
                println("Resource not found: $requestedFile")

                // Send a response indicating that the resource was not found
                sendText(session, "resource not found")
            }
        } catch (e: Exception) {
            // Handle exceptions, log an error, and inform the client about the error
            println("Error handling GET request: ${e.message}")
            sendText(session, "error handling GET request: ${e.message}")
        }
    }

    private suspend fun handlePostRequest(session: DefaultWebSocketSession, request: String) {

        val newFile = controller.fileExist(request, true)

        if (!newFile.isDirectory) {
            val frame = session.incoming.receive()
            if (frame is Frame.Text) {
                val message = frame.readText()
                val fileData = encryption.decryptFile(message)
                saveFileToServer(newFile, fileData.toByteArray(), session)
            }
            return
        } else handleGetRequest(session, "GET:${pointer}")
    }

    private suspend fun handleDeleteRequest(session: DefaultWebSocketSession, request: String) {
        val file = context.getExternalFilesDir("${BASE_DIRECTORY}/${request}")

        file?.delete()

        println("Deleted file: $request")
        handleGetRequest(session, pointer)
    }

    private fun listFileMetadata(directory: File): MetadataResponse {
        val items = mutableListOf<Metadata>()

        directory.listFiles()?.forEach { item ->
            val relativePath = home?.let { item.relativeTo(it).path }
            val itemType = if (item.isFile) "file" else "folder"

            val metadata = Metadata(item.name,
                relativePath ?: "",
                itemType,
                if (item.isFile) item.length().toInt() else 0,
                item.lastModified() / 1000.0
            )

            items.add(metadata)
        }

        return MetadataResponse(items)
    }

    private suspend fun saveFileToServer(file: File, fileData: ByteArray, session: DefaultWebSocketSession) {
        try {
            file.writeBytes(fileData)
            handleGetRequest(session, "GET:${pointer}")
        } catch (e: Exception) {
            println("Error saving file: ${e.message}")
        }
    }

    private suspend fun handleSearchRequest(session: DefaultWebSocketSession, request: String) {
        val searchQuery = request
        val searchRegex = Regex(searchQuery, RegexOption.IGNORE_CASE)

        val pointerDirectory = if (pointer.isNullOrBlank()) home else context.getExternalFilesDir("${BASE_DIRECTORY}/${pointer}")

        val searchResults = pointerDirectory?.let { searchFiles(it, searchRegex) }

        if (searchResults != null) {
            sendText(session, "$request;${Gson().toJson(searchResults)}")
            sendFilePreviews(session, searchResults)
        }
    }

    private fun searchFiles(searchFolder: File, searchRegex: Regex): MetadataResponse {
        val items = mutableListOf<Metadata>()

        searchFolder.listFiles()?.forEach { item ->
            val relativePath = home?.let { item.relativeTo(it).path }
            val itemType = if (item.isFile) "file" else "folder"

            if (searchRegex.containsMatchIn(item.name)) {
                val metadata = Metadata( item.name, relativePath ?: "", itemType, if (item.isFile) item.length().toInt() else 0, item.lastModified() / 1000.0 )

                items.add(metadata)
            }

            if (item.isDirectory) {
                val subdirectoryResults = searchFiles(item, searchRegex)
                items.addAll(subdirectoryResults.items)
            }
        }

        return MetadataResponse(items)
    }

    private suspend fun handleUpdateRequest(session: DefaultWebSocketSession, request: String) {
        val (sourcePath, destinationPath) = request.split(";")
        val sourceFile = context.getExternalFilesDir("${BASE_DIRECTORY}${sourcePath}")

        try {
            if (sourceFile?.exists() == true) {
                val destinationFile = controller.fileExist(destinationPath, true)
                sourceFile.copyTo(destinationFile, true)
                if (destinationFile.exists())
                    sourceFile.delete()
            } else {
                println("Source path does not exist: $sourcePath")
            }
        } catch (error: Exception) {
            println("Error moving file/directory: ${error.message}")
        }
        handleGetRequest(session, pointer)
    }

    private suspend fun sendFilePreviews(session: DefaultWebSocketSession, items: MetadataResponse) {
        for (item in items.items) {
            val filePath = Paths.get(BASE_DIRECTORY, item.path.toString()).toString()
            val fileExtension = File(filePath).extension
            var preview: String? = null

            if (fileExtension.matches(Regex("(jpg|png|gif)$", RegexOption.IGNORE_CASE))) {
                preview = generateImagePreview(filePath)
            }

            if (preview != null) {
                println("Sending preview for file: ${item.name}")
                sendText(session, "base64;${item.path};$preview")
            }
        }
    }

    private fun generateImagePreview(filePath: String): String? {
        try {
            val imageFile = context.getExternalFilesDir(filePath)
            if (imageFile != null) {
                val imageThumbnail = ThumbnailUtils.extractThumbnail(
                    BitmapFactory.decodeFile(imageFile.path),
                    120, 150
                )

                val byteArrayOutputStream = ByteArrayOutputStream()
                imageThumbnail.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                return Base64.getEncoder().encodeToString(byteArray)
            }
        } catch (error: Exception) {
            println("Error generating preview for $filePath: ${error.message}")
        }
        return null
    }
}
