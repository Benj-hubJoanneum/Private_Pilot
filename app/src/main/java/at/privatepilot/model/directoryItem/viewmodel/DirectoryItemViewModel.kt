package at.privatepilot.model.directoryItem.viewmodel

import androidx.lifecycle.ViewModel
import at.privatepilot.model.directoryItem.DirectoryItem

class DirectoryItemViewModel(directoryItem: DirectoryItem) : ViewModel() {
    var name: String = directoryItem.name
    val path: String = directoryItem.path
    val depth: Int = directoryItem.depth
}
