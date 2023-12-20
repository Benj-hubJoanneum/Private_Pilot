package at.privatepilot.model.nodeItem

import android.graphics.Bitmap
import at.privatepilot.model.FileType
import at.privatepilot.model.INode
import at.privatepilot.restapi.model.IMetadata

data class NodeItem(
    override var name: String,
    override var path: String = name,
    val description: String = "",
    var depth: Int = 0,
    var bitmap: Bitmap? = null,
    override val last_modified: Double = 0.0,
    override val size: Int = 0
) : INode, IMetadata, Comparable<NodeItem> {

    override var type: FileType = setByType()
    override var parentFolder: String = setParent()

    private fun setByType(): FileType {
        val extension = name.substringAfterLast('.', "")
        return try {
            if (extension.isBlank())
                FileType.FOLDER
            else FileType.valueOf(extension.uppercase())
        } catch (e: IllegalArgumentException) {
            FileType.DOC
        }
    }

    private fun setParent(): String {
        return path.split('/')[0]
    }

    override fun compareTo(other: NodeItem): Int {
        return this.path.compareTo(other.path)
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeItem) return false
        return path == other.path
    }
    override fun hashCode(): Int {
        return path.hashCode()
    }
}
