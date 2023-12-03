package at.privatepilot.model.directoryItem

import at.privatepilot.model.FileType
import at.privatepilot.model.ITreeNode
import at.privatepilot.restapi.model.IMetadata

data class DirectoryItem(
    override var name: String,
    override var path: String = name,
    override var depth: Int = 0,
    override val last_modified: Double = 0.0,
    override val size: Int = 0,
    override var type: FileType = FileType.FOLDER
) : ITreeNode, IMetadata, Comparable<DirectoryItem> {

    override var parentFolder: String = setParent()

    private fun setParent(): String {
        return path.split("/").dropLast(1).joinToString("/")
    }
    override fun compareTo(other: DirectoryItem): Int {
        return this.path.compareTo(other.path)
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectoryItem) return false
        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
