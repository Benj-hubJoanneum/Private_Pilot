package at.privatepilot.restapi.controller

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import at.privatepilot.model.INode
import at.privatepilot.model.directoryItem.DirectoryItem
import at.privatepilot.model.nodeItem.NodeItem
import at.privatepilot.restapi.client.WebSocketClient
import at.privatepilot.restapi.model.MetadataResponse
import at.privatepilot.restapi.service.NodeRepository
import com.google.gson.Gson
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.IOException
import java.util.Base64

class ControllerSocket(
    private val nodeRepository: NodeRepository,
    private val callback: ControllerCallback) :
    WebSocketClient.WebSocketCallback {
    private val webSocketClient: WebSocketClient = WebSocketClient(this)
    private var context: Context? = null

    fun createNodes(url: String, file: File? = null) {
        val filepath = "$url/${file?.name ?: ""}"
        sendToServer("POST", filepath) // send request to mkdir of filepath

        if (file != null) {
            val byteString = file.parseFileToBytes()
            if (byteString != null) sendToServer(byteString) // send file to server
        }
    }

    fun requestNodes(url: String) {
        sendToServer("GET", url)
    }

    private fun readNodes(json: String) {
        try {
            val pointer = json.substringBefore(';')
            val nodes = json.substringAfter(';')

            val directoryList = mutableSetOf<DirectoryItem>()
            val nodeList: MutableSet<INode>
            val data = nodes.parseItemsFromResponse()

            directoryList.addAll(data.items.filter { it.type == "folder" }.map {
                DirectoryItem(it.name,"/${it.path}")
            })
            nodeList = data.items.map { NodeItem(it.name, "/${it.path}") }.toMutableSet()

            callback.onControllerSourceChanged(pointer, directoryList, nodeList)
        } catch (e: IOException) {
            println("Error parsing JSON: ${e.message}")
        }
    }

    private fun readBase64(base64String: String) {
        val path = base64String.substringBeforeLast(';')
        val base64 = base64String.substringAfterLast(';')

        val decodedBytes = Base64.getDecoder().decode(base64)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        callback.onPreviewReceived("/$path", bitmap)
    }

    fun updateNodes(context: Context, urlSource: String, urlTarget: String) {
        this.context = context
        val sourceFile = fileExist(urlSource, context)
        val targetFile = fileExist(urlTarget, context, write = true)

        if (sourceFile.exists()) {
            val finalDestinationPath = if (sourceFile.isDirectory) {
                File(targetFile, sourceFile.name)
            } else {
                targetFile
            }

            moveFileOrDirectory(sourceFile, finalDestinationPath)
            sendToServer("UPDATE", "$urlSource;$urlTarget")
        } else {
            println("Source file or directory does not exist: $urlSource")
        }
    }

    fun deleteNodes(url: String) {
        sendToServer("DELETE", url)
    }

    fun downloadFile(url: String) {
        sendToServer("GET", url)
    }

    fun fileExist(url: String, context: Context, write: Boolean = false): File {
        val fileName = url.substringAfterLast('/')
        val filepath = url.substringBeforeLast('/')
        val dirPath = "public_$filepath"

        this.context = context

        val file = context.getExternalFilesDir(dirPath)

        if (write)
            file?.mkdirs()

        return File(file, fileName)
    }

    fun openFile(url: String) {
        if (context != null) {
            val file = fileExist(url, context!!)

            if (file.exists()) {
                val mimeType = context!!.contentResolver.getType(Uri.fromFile(file))
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = FileProvider.getUriForFile(context!!, context!!.applicationContext.packageName + ".provider", file)

                intent.setDataAndType(uri, mimeType)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                try {
                    context!!.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendSearchRequest(query: String) {
        sendToServer("FIND", query)
    }

    private fun moveFileOrDirectory(source: File, destination: File) {
        try {
            if (source.isDirectory) {
                destination.mkdirs()
                source.listFiles()?.forEach { file ->
                    moveFileOrDirectory(file, File(destination, file.name))
                }
            } else {
                source.copyTo(destination, true)
                source.delete()
            }
        } catch (e: IOException) {
            println("Error moving file/directory: ${e.message}")
        }
    }

    private fun String.parseItemsFromResponse(): MetadataResponse {
        return try {
            Gson().fromJson(this, MetadataResponse::class.java)
        } catch (e: Exception) {
            MetadataResponse(listOf())
        }
    }

    private fun ByteString.parseBytesToFile(file: File) {
        return try {
            val byteArray = this.toByteArray()
            file.writeBytes(byteArray)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun File.parseFileToBytes(): ByteString? {
        return try {
            this.readBytes().toByteString()
        } catch (e: IOException) {
            e.printStackTrace()
            ByteString.EMPTY
        }
    }

    private fun saveFileToPhone(message: ByteString) {
        if (context != null) {
            try {
                if (message.size > 0) {
                    val pointer = nodeRepository.filePointer

                    // create file on phone
                    val outputFile = fileExist(pointer, this.context!!)

                    // write data to file
                    message.parseBytesToFile(outputFile)

                    callback.onFileReceived()

                }
            } catch (e: IOException) {
                println("Error saving file: ${e.message}")
            }
        }
    }

    private fun sendToServer(prefix: String, content: String) {
        val requestMessage = "$prefix:$content"
        webSocketClient.sendToServer(requestMessage)
    }

    private fun sendToServer(requestMessage: ByteString) {
        webSocketClient.sendToServer(requestMessage)
    }

    override fun onMessageReceived(message: String) {
        if (message.startsWith("base64;")) {
            readBase64(message.substring("base64;".length))
        } else {
            readNodes(message)
        }
    }

    override fun onMessageReceived(message: ByteString) {
        saveFileToPhone(message)
    }

    override fun onConnection() {
        callback.onConnection()
    }

    override fun onConnectionCancel() {
        callback.onConnectionCancel()
    }

    override fun onConnectionFailure() {
        callback.onConnectionFailure()
    }

    interface ControllerCallback {
        fun onControllerSourceChanged(pointer: String, directoryList : MutableSet<DirectoryItem>, nodeList: MutableSet<INode>)
        fun onPreviewReceived(path : String, bitmap: Bitmap)
        fun onFileReceived()
        fun onConnection()
        fun onConnectionCancel()
        fun onConnectionFailure()
    }
}
