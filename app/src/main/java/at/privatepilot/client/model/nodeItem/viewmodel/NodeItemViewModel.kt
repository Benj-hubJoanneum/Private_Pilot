package at.privatepilot.model.nodeItem.viewmodel

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import at.privatepilot.R
import at.privatepilot.model.FileType
import at.privatepilot.model.nodeItem.NodeItem

class NodeItemViewModel(nodeItem: NodeItem) : ViewModel() {
    var name: String = if (nodeItem.name.contains('/')) {
        nodeItem.name.substringAfterLast('/')
    } else {
        nodeItem.name
    }
    val type: FileType = nodeItem.type
    val path: String = nodeItem.path
    val drawable: Int = getItemImage()
    var bitmap : Bitmap? = nodeItem.bitmap
    var icon : Drawable? = null

    private fun getItemImage(): Int {
        return when (type) {
            FileType.PDF -> R.drawable.ic_pdf
            FileType.TXT -> R.drawable.ic_text
            FileType.XLSX -> R.drawable.ic_table
            FileType.JPG -> R.drawable.ic_image
            FileType.JPEG -> R.drawable.ic_image
            FileType.PNG -> R.drawable.ic_image
            FileType.DOC -> R.drawable.ic_document
            FileType.FOLDER -> R.drawable.ic_folder
            else -> R.drawable.ic_document
        }
    }
}
