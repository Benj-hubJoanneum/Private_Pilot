package at.privatepilot.restapi.service

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.privatepilot.model.FileType
import at.privatepilot.model.INode
import at.privatepilot.model.directoryItem.DirectoryItem
import at.privatepilot.model.nodeItem.NodeItem
import at.privatepilot.restapi.controller.ControllerSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class NodeRepository() : ControllerSocket.ControllerCallback {

    companion object {
        @Volatile
        private var instance: NodeRepository? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: NodeRepository().also { instance = it }
            }
    }

    //collections
    private var controllerNode = ControllerSocket(this, this)
    var selectedFileUri: Uri? = null

    //all directories
    private val _directoryList = MutableLiveData<MutableSet<DirectoryItem>>()
    val directoryList: LiveData<MutableSet<DirectoryItem>> = _directoryList

    //opened directory
    private val _directoryPointer = MutableLiveData<String>()
    val directoryPointer: LiveData<String> = _directoryPointer

    //all files in opened directory
    private var fullFileList: MutableSet<INode> = mutableSetOf()

    //actual shown nodes
    private val _displayedList = MutableLiveData<MutableList<INode>>()
    val displayedList: LiveData<MutableList<INode>> = _displayedList

    //filemanagement
    var filePointer = ""
    var cutItems = mutableListOf<String>()
    var selectedItems = mutableListOf<Int>()

    //listener
    private var downloadCallback: DownloadCallback? = null
    private var webSocketCallback: ConnectionCallback? = null

    fun launchFileSelection(openFileLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"

        openFileLauncher.launch(intent)
    }

    fun onSearchQuery(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            controllerNode.sendSearchRequest(query)
        }
    }

    fun undoSearch(path: String) {
        var newPath = path
        if (path.isBlank())
            newPath = directoryPointer.value as String

        CoroutineScope(Dispatchers.IO).launch {
            controllerNode.requestNodes(newPath)
        }
    }

    fun createNode(file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            val path = _directoryPointer.value ?: ""
            controllerNode.createNodes(path, file)
        }
    }

    fun createNode(folder: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val path = _directoryPointer.value ?: ""
            controllerNode.createNodes("$path/$folder")
        }
    }

    fun readNode(path: String? = directoryPointer.value) {
        CoroutineScope(Dispatchers.IO).launch {
            if (path != null) {
                showLoadingOverlay()
                controllerNode.requestNodes(path)
            }
        }
    }

    fun moveNodes(context: Context, path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val pointer = directoryPointer.value ?: ""
            controllerNode.updateNodes(context, path, pointer)
            controllerNode.requestNodes(pointer)
            directoryListRemoveEntry(path)
        }
    }

    fun moveNodes(context: Context, path: String, newName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val pointer = directoryPointer.value ?: ""
            controllerNode.updateNodes(context, path, newName)
            controllerNode.requestNodes(pointer)
            directoryListRemoveEntry(path)
        }
    }

    fun deleteNode(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            controllerNode.deleteNodes(path)
        }
    }

    fun downloadFile(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            filePointer = path
            controllerNode.downloadFile(path)
        }
    }

    private fun directoryListRemoveEntry(path : String) {
        val directoryList = _directoryList.value ?: mutableSetOf()
        directoryList.removeIf{ it.path == path }
        _directoryList.postValue(directoryList.toMutableSet())
    }

    private fun directoryListAddByParent(list : MutableSet<DirectoryItem>) {
        val directoryList = _directoryList.value ?: mutableSetOf()

        list -= directoryList
        val newList = directoryList.toMutableList()

        for (newNode in list.sortedByDescending { it.name }){
            val index = directoryList.indexOfFirst { it.path == newNode.parentFolder }
            newList.add(index + 1, newNode)
        }
        _directoryList.postValue(newList.toMutableSet())
    }

    private fun displayListSorting(){
        val sortedList = fullFileList
            .sortedWith(compareBy(
                { it.type != FileType.FOLDER },
                { it.name }))
            .toMutableList()
        _displayedList.postValue(sortedList)
    }

    fun fileExist(url: String, context: Context): Boolean {
        return controllerNode.fileExist(url, context).exists()
    }

    fun openFile(filePath: String) {
        controllerNode.openFile(filePath)
    }

    fun getThisFile(uri: Uri?, context: Context): File {
        uri ?: return File("")
        val resolver = context.contentResolver

        val inputStream = resolver.openInputStream(uri)
        val file = File(context.cacheDir, resolver.getFileName(uri))

        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }

        return file
    }

    private fun ContentResolver.getFileName(uri: Uri?): String {
        uri ?: return ""

        val cursor = query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return ""
    }

    private fun updateNodePreview(path: String, bitmap: Bitmap) {
        val nodeList = _displayedList.value ?: mutableListOf()

        val nodeToUpdate = nodeList.find { it.path == path }

        if (nodeToUpdate != null && nodeToUpdate is NodeItem) {
            nodeToUpdate.bitmap = bitmap

            _displayedList.postValue(nodeList)
        }
    }

    override fun onControllerSourceChanged(pointer : String, directoryList : MutableSet<DirectoryItem>, nodeList : MutableSet<INode>) {
        fullFileList = nodeList
        directoryListAddByParent(directoryList)
        displayListSorting()

        _directoryPointer.postValue(pointer)
    }

    override fun onPreviewReceived(path: String, bitmap: Bitmap) {
        updateNodePreview(path, bitmap)
    }

    override fun onFileReceived() {
        downloadCallback?.onDownloadFinished()
    }

    override fun onConnection() {
        webSocketCallback?.onConnection()
    }

    override fun onConnectionCancel() {
        webSocketCallback?.onConnectionCancel()
    }

    override fun onConnectionFailure() {
        webSocketCallback?.onConnectionFailure()
    }

    fun setDownloadCallback(callback: DownloadCallback) {
        downloadCallback = callback
    }

    fun setWebsocketCallback(callback: ConnectionCallback) {
        webSocketCallback = callback
    }

    interface ConnectionCallback {
        fun onConnection()
        fun onConnectionCancel()
        fun onConnectionFailure()
    }
    interface DownloadCallback {
        fun onDownloadFinished()
    }

    private lateinit var loadingCallback: LoadingCallback

    private fun showLoadingOverlay() {
        loadingCallback.showLoadingOverlay()
    }

    fun setLoadingCallback(callback: LoadingCallback) {
        loadingCallback = callback
    }

    interface LoadingCallback {
        fun showLoadingOverlay()
    }
}
