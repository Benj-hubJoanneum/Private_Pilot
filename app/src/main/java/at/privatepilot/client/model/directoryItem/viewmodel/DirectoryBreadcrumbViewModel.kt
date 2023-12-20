package at.privatepilot.model.directoryItem.viewmodel

import android.graphics.Color
import android.view.View
import androidx.lifecycle.ViewModel
import at.privatepilot.model.directoryItem.DirectoryItem

class DirectoryBreadcrumbViewModel(directoryItem: DirectoryItem) : ViewModel() {
    var name: String = directoryItem.name
    val path: String = directoryItem.path
    var color: Int = Color.BLACK
    var divider: Int = View.VISIBLE
}
