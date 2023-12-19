package at.privatepilot.server

import android.content.Context
import java.io.File

class Controller(private val context: Context) {

    fun fileExist(url: String, write: Boolean = false): File {
        val filepath = url.substringBeforeLast('/')
        val fileName = url.substringAfterLast('/')
        val dirPath = "server_$filepath"

        val file = context.getExternalFilesDir(dirPath)

        if (write)
            file?.mkdirs()

        return File(file, fileName)
    }
}